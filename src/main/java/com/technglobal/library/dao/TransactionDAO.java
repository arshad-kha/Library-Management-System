package com.technglobal.library.dao;

import com.technglobal.library.db.DBConnection;
import com.technglobal.library.exception.LibraryException;
import com.technglobal.library.model.Transaction;

import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles borrow/return workflow. This is the DAO that shows off real
 * transactional integrity: issuing a book has to (a) insert a transaction
 * row AND (b) decrement available_copies — both or neither. We use a
 * single JDBC connection with autoCommit=false and roll back on any error
 * so the two tables can never drift out of sync.
 */
public class TransactionDAO {

    private static final int LOAN_PERIOD_DAYS = 14;
    private static final double FINE_PER_DAY = 5.00; // e.g. ₹5/day late fee

    public void issueBook(int bookId, int memberId) throws LibraryException {
        String checkAvailabilitySql = "SELECT available_copies FROM books WHERE book_id=? FOR UPDATE";
        String decrementSql = "UPDATE books SET available_copies = available_copies - 1 WHERE book_id=?";
        String insertTransactionSql =
            "INSERT INTO transactions (book_id, member_id, issue_date, due_date, status) VALUES (?, ?, ?, ?, 'ISSUED')";

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            int available;
            try (PreparedStatement ps = conn.prepareStatement(checkAvailabilitySql)) {
                ps.setInt(1, bookId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new LibraryException("Book not found.");
                    }
                    available = rs.getInt("available_copies");
                }
            }

            if (available <= 0) {
                throw new LibraryException("No copies of this book are currently available.");
            }

            try (PreparedStatement ps = conn.prepareStatement(decrementSql)) {
                ps.setInt(1, bookId);
                ps.executeUpdate();
            }

            LocalDate issueDate = LocalDate.now();
            LocalDate dueDate = issueDate.plusDays(LOAN_PERIOD_DAYS);
            try (PreparedStatement ps = conn.prepareStatement(insertTransactionSql)) {
                ps.setInt(1, bookId);
                ps.setInt(2, memberId);
                ps.setDate(3, Date.valueOf(issueDate));
                ps.setDate(4, Date.valueOf(dueDate));
                ps.executeUpdate();
            }

            conn.commit();

        } catch (LibraryException e) {
            rollbackQuietly(conn);
            throw e;
        } catch (SQLException e) {
            rollbackQuietly(conn);
            throw new LibraryException("Database error while issuing book: " + e.getMessage(), e);
        } finally {
            closeQuietly(conn);
        }
    }

    public void returnBook(int transactionId) throws LibraryException {
        String getTransactionSql = "SELECT book_id, due_date, status FROM transactions WHERE transaction_id=? FOR UPDATE";
        String incrementSql = "UPDATE books SET available_copies = available_copies + 1 WHERE book_id=?";
        String updateTransactionSql =
            "UPDATE transactions SET return_date=?, fine_amount=?, status='RETURNED' WHERE transaction_id=?";

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            int bookId;
            LocalDate dueDate;
            String status;
            try (PreparedStatement ps = conn.prepareStatement(getTransactionSql)) {
                ps.setInt(1, transactionId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new LibraryException("Transaction not found.");
                    }
                    bookId = rs.getInt("book_id");
                    dueDate = rs.getDate("due_date").toLocalDate();
                    status = rs.getString("status");
                }
            }

            if ("RETURNED".equals(status)) {
                throw new LibraryException("This book has already been returned.");
            }

            LocalDate returnDate = LocalDate.now();
            long daysLate = ChronoUnit.DAYS.between(dueDate, returnDate);
            double fine = daysLate > 0 ? daysLate * FINE_PER_DAY : 0.0;

            try (PreparedStatement ps = conn.prepareStatement(updateTransactionSql)) {
                ps.setDate(1, Date.valueOf(returnDate));
                ps.setDouble(2, fine);
                ps.setInt(3, transactionId);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(incrementSql)) {
                ps.setInt(1, bookId);
                ps.executeUpdate();
            }

            conn.commit();

        } catch (LibraryException e) {
            rollbackQuietly(conn);
            throw e;
        } catch (SQLException e) {
            rollbackQuietly(conn);
            throw new LibraryException("Database error while returning book: " + e.getMessage(), e);
        } finally {
            closeQuietly(conn);
        }
    }

    public List<Transaction> getAllTransactions() throws LibraryException {
        return getAllTransactions(null);
    }

    /** Optional statusFilter: "ISSUED", "RETURNED", "OVERDUE", or null for all. */
    public List<Transaction> getAllTransactions(String statusFilter) throws LibraryException {
        StringBuilder sql = new StringBuilder(
            "SELECT t.*, b.title AS book_title, m.name AS member_name " +
            "FROM transactions t " +
            "JOIN books b ON t.book_id = b.book_id " +
            "JOIN members m ON t.member_id = m.member_id ");

        boolean hasFilter = statusFilter != null && !statusFilter.isBlank() && !statusFilter.equalsIgnoreCase("ALL");
        if (hasFilter) {
            sql.append("WHERE t.status = ? ");
        }
        sql.append("ORDER BY t.issue_date DESC");

        List<Transaction> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            if (hasFilter) {
                ps.setString(1, statusFilter.toUpperCase());
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new LibraryException("Database error while fetching transactions: " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Marks any ISSUED transaction whose due_date has passed as OVERDUE.
     * Called on app startup / refresh so the status column stays accurate
     * without needing a scheduled job.
     */
    public void refreshOverdueStatuses() throws LibraryException {
        String sql = "UPDATE transactions SET status='OVERDUE' WHERE status='ISSUED' AND due_date < ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(LocalDate.now()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new LibraryException("Database error while refreshing overdue statuses: " + e.getMessage(), e);
        }
    }

    private Transaction mapRow(ResultSet rs) throws SQLException {
        Transaction t = new Transaction();
        t.setTransactionId(rs.getInt("transaction_id"));
        t.setBookId(rs.getInt("book_id"));
        t.setMemberId(rs.getInt("member_id"));
        t.setBookTitle(rs.getString("book_title"));
        t.setMemberName(rs.getString("member_name"));
        t.setIssueDate(rs.getDate("issue_date").toLocalDate());
        t.setDueDate(rs.getDate("due_date").toLocalDate());
        Date returnDate = rs.getDate("return_date");
        if (returnDate != null) {
            t.setReturnDate(returnDate.toLocalDate());
        }
        t.setFineAmount(rs.getDouble("fine_amount"));
        t.setStatus(Transaction.Status.valueOf(rs.getString("status")));
        return t;
    }

    private void rollbackQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException ignored) {
                // best-effort rollback
            }
        }
    }

    private void closeQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.setAutoCommit(true);
                conn.close();
            } catch (SQLException ignored) {
                // best-effort close
            }
        }
    }
}
