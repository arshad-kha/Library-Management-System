package com.technglobal.library.ui;

import com.technglobal.library.dao.BookDAO;
import com.technglobal.library.dao.MemberDAO;
import com.technglobal.library.dao.TransactionDAO;
import com.technglobal.library.exception.LibraryException;
import com.technglobal.library.model.Book;
import com.technglobal.library.model.Member;
import com.technglobal.library.model.Transaction;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class TransactionsPanel extends JPanel {

    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final BookDAO bookDAO = new BookDAO();
    private final MemberDAO memberDAO = new MemberDAO();

    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JComboBox<String> statusFilter = new JComboBox<>(
        new String[]{"ALL", "ISSUED", "RETURNED", "OVERDUE"});

    public TransactionsPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("Filter by status:"));
        topPanel.add(statusFilter);
        JButton filterBtn = new JButton("Apply");
        JButton refreshBtn = new JButton("Refresh");
        topPanel.add(filterBtn);
        topPanel.add(refreshBtn);
        add(topPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(
            new Object[]{"Txn ID", "Book", "Member", "Issue Date", "Due Date", "Return Date", "Fine (₹)", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setRowHeight(24);
        table.setAutoCreateRowSorter(true);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton issueBtn = new JButton("Issue Book");
        JButton returnBtn = new JButton("Return Selected");
        bottomPanel.add(issueBtn);
        bottomPanel.add(returnBtn);
        add(bottomPanel, BorderLayout.SOUTH);

        filterBtn.addActionListener(e -> loadTransactions());
        refreshBtn.addActionListener(e -> loadTransactions());
        issueBtn.addActionListener(e -> showIssueDialog());
        returnBtn.addActionListener(e -> returnSelected());

        loadTransactions();
    }

    private void loadTransactions() {
        try {
            transactionDAO.refreshOverdueStatuses();
            String status = (String) statusFilter.getSelectedItem();
            List<Transaction> transactions = transactionDAO.getAllTransactions(status);
            tableModel.setRowCount(0);
            for (Transaction t : transactions) {
                tableModel.addRow(new Object[]{
                    t.getTransactionId(), t.getBookTitle(), t.getMemberName(),
                    t.getIssueDate(), t.getDueDate(),
                    t.getReturnDate() == null ? "-" : t.getReturnDate(),
                    String.format("%.2f", t.getFineAmount()),
                    t.getStatus()
                });
            }
        } catch (LibraryException ex) {
            showError(ex.getMessage());
        }
    }

    private void showIssueDialog() {
        try {
            List<Book> books = bookDAO.getAllBooks();
            List<Member> members = memberDAO.getAllMembers();

            if (books.isEmpty() || members.isEmpty()) {
                showInfo("You need at least one book and one member before issuing a loan.");
                return;
            }

            JComboBox<Book> bookBox = new JComboBox<>(books.toArray(new Book[0]));
            JComboBox<Member> memberBox = new JComboBox<>(members.toArray(new Member[0]));

            JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
            panel.add(new JLabel("Book:"));
            panel.add(bookBox);
            panel.add(new JLabel("Member:"));
            panel.add(memberBox);

            int result = JOptionPane.showConfirmDialog(this, panel, "Issue Book",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (result != JOptionPane.OK_OPTION) return;

            Book selectedBook = (Book) bookBox.getSelectedItem();
            Member selectedMember = (Member) memberBox.getSelectedItem();

            transactionDAO.issueBook(selectedBook.getBookId(), selectedMember.getMemberId());
            loadTransactions();

        } catch (LibraryException ex) {
            showError(ex.getMessage());
        }
    }

    private void returnSelected() {
        int row = table.getSelectedRow();
        if (row == -1) {
            showInfo("Please select a transaction to return.");
            return;
        }
        row = table.convertRowIndexToModel(row);
        int transactionId = (int) tableModel.getValueAt(row, 0);
        String status = tableModel.getValueAt(row, 7).toString();

        if ("RETURNED".equals(status)) {
            showInfo("This book has already been returned.");
            return;
        }

        try {
            transactionDAO.returnBook(transactionId);
            showInfo("Book returned. Any applicable late fine has been calculated.");
            loadTransactions();
        } catch (LibraryException ex) {
            showError(ex.getMessage());
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "Info", JOptionPane.INFORMATION_MESSAGE);
    }
}
