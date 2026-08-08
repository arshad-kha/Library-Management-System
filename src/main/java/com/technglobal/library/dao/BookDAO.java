package com.technglobal.library.dao;

import com.technglobal.library.db.DBConnection;
import com.technglobal.library.exception.LibraryException;
import com.technglobal.library.model.Book;
import com.technglobal.library.util.ValidationUtil;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for the `books` table.
 * All SQL lives here — the UI never talks to JDBC directly.
 * Every query uses PreparedStatement to prevent SQL injection.
 */
public class BookDAO {

    public void addBook(Book book) throws LibraryException {
        ValidationUtil.requireNonBlank(book.getTitle(), "Title");
        ValidationUtil.requireNonBlank(book.getAuthor(), "Author");
        ValidationUtil.validateIsbn(book.getIsbn());
        ValidationUtil.validatePositiveInt(book.getTotalCopies(), "Total copies");

        String sql = "INSERT INTO books (isbn, title, author, genre, total_copies, available_copies) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, book.getIsbn().trim());
            ps.setString(2, book.getTitle().trim());
            ps.setString(3, book.getAuthor().trim());
            ps.setString(4, book.getGenre() == null ? "" : book.getGenre().trim());
            ps.setInt(5, book.getTotalCopies());
            ps.setInt(6, book.getTotalCopies()); // available = total on creation
            ps.executeUpdate();

        } catch (SQLIntegrityConstraintViolationException e) {
            throw new LibraryException("A book with this ISBN already exists.", e);
        } catch (SQLException e) {
            throw new LibraryException("Database error while adding book: " + e.getMessage(), e);
        }
    }

    public void updateBook(Book book) throws LibraryException {
        ValidationUtil.requireNonBlank(book.getTitle(), "Title");
        ValidationUtil.requireNonBlank(book.getAuthor(), "Author");
        ValidationUtil.validateIsbn(book.getIsbn());

        String sql = "UPDATE books SET isbn=?, title=?, author=?, genre=?, total_copies=?, available_copies=? " +
                     "WHERE book_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, book.getIsbn().trim());
            ps.setString(2, book.getTitle().trim());
            ps.setString(3, book.getAuthor().trim());
            ps.setString(4, book.getGenre());
            ps.setInt(5, book.getTotalCopies());
            ps.setInt(6, book.getAvailableCopies());
            ps.setInt(7, book.getBookId());

            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new LibraryException("No book found with ID " + book.getBookId());
            }
        } catch (SQLException e) {
            throw new LibraryException("Database error while updating book: " + e.getMessage(), e);
        }
    }

    public void deleteBook(int bookId) throws LibraryException {
        String sql = "DELETE FROM books WHERE book_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, bookId);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new LibraryException("No book found with ID " + bookId);
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new LibraryException("Cannot delete this book — it has transaction history.", e);
        } catch (SQLException e) {
            throw new LibraryException("Database error while deleting book: " + e.getMessage(), e);
        }
    }

    public List<Book> getAllBooks() throws LibraryException {
        return getAllBooks(null, "title");
    }

    /**
     * Fetch books with optional free-text search (matches title/author/genre/ISBN)
     * and a sort column. Powers the search/filter/sort requirement.
     */
    public List<Book> getAllBooks(String searchTerm, String sortColumn) throws LibraryException {
        List<String> allowedSortColumns = List.of("title", "author", "genre", "total_copies", "available_copies");
        String orderBy = allowedSortColumns.contains(sortColumn) ? sortColumn : "title";

        StringBuilder sql = new StringBuilder("SELECT * FROM books");
        boolean hasSearch = searchTerm != null && !searchTerm.trim().isEmpty();
        if (hasSearch) {
            sql.append(" WHERE title LIKE ? OR author LIKE ? OR genre LIKE ? OR isbn LIKE ?");
        }
        sql.append(" ORDER BY ").append(orderBy);

        List<Book> books = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            if (hasSearch) {
                String like = "%" + searchTerm.trim() + "%";
                ps.setString(1, like);
                ps.setString(2, like);
                ps.setString(3, like);
                ps.setString(4, like);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    books.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new LibraryException("Database error while fetching books: " + e.getMessage(), e);
        }
        return books;
    }

    public Book getBookById(int bookId) throws LibraryException {
        String sql = "SELECT * FROM books WHERE book_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw new LibraryException("Database error while fetching book: " + e.getMessage(), e);
        }
        throw new LibraryException("No book found with ID " + bookId);
    }

    private Book mapRow(ResultSet rs) throws SQLException {
        Book b = new Book();
        b.setBookId(rs.getInt("book_id"));
        b.setIsbn(rs.getString("isbn"));
        b.setTitle(rs.getString("title"));
        b.setAuthor(rs.getString("author"));
        b.setGenre(rs.getString("genre"));
        b.setTotalCopies(rs.getInt("total_copies"));
        b.setAvailableCopies(rs.getInt("available_copies"));
        Date added = rs.getDate("added_date");
        if (added != null) {
            b.setAddedDate(LocalDate.parse(added.toString()));
        }
        return b;
    }
}
