package com.technglobal.library.ui;

import com.technglobal.library.dao.BookDAO;
import com.technglobal.library.exception.LibraryException;
import com.technglobal.library.model.Book;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class BooksPanel extends JPanel {

    private final BookDAO bookDAO = new BookDAO();
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JTextField searchField = new JTextField(20);
    private final JComboBox<String> sortBox = new JComboBox<>(
        new String[]{"title", "author", "genre", "total_copies", "available_copies"});

    public BooksPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ---- Top: search + sort + actions ----
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("Search:"));
        topPanel.add(searchField);
        topPanel.add(new JLabel("Sort by:"));
        topPanel.add(sortBox);
        JButton searchBtn = new JButton("Search");
        JButton clearBtn = new JButton("Clear");
        topPanel.add(searchBtn);
        topPanel.add(clearBtn);
        add(topPanel, BorderLayout.NORTH);

        // ---- Center: table ----
        tableModel = new DefaultTableModel(
            new Object[]{"ID", "ISBN", "Title", "Author", "Genre", "Total", "Available"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setRowHeight(24);
        table.setAutoCreateRowSorter(true);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // ---- Bottom: CRUD buttons ----
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addBtn = new JButton("Add Book");
        JButton editBtn = new JButton("Edit Selected");
        JButton deleteBtn = new JButton("Delete Selected");
        JButton refreshBtn = new JButton("Refresh");
        bottomPanel.add(addBtn);
        bottomPanel.add(editBtn);
        bottomPanel.add(deleteBtn);
        bottomPanel.add(refreshBtn);
        add(bottomPanel, BorderLayout.SOUTH);

        // ---- Wiring ----
        searchBtn.addActionListener(e -> loadBooks());
        clearBtn.addActionListener(e -> {
            searchField.setText("");
            loadBooks();
        });
        refreshBtn.addActionListener(e -> loadBooks());
        addBtn.addActionListener(e -> showBookDialog(null));
        editBtn.addActionListener(e -> {
            Book selected = getSelectedBook();
            if (selected == null) {
                showInfo("Please select a book to edit.");
                return;
            }
            showBookDialog(selected);
        });
        deleteBtn.addActionListener(e -> {
            Book selected = getSelectedBook();
            if (selected == null) {
                showInfo("Please select a book to delete.");
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(this,
                "Delete \"" + selected.getTitle() + "\"? This cannot be undone.",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    bookDAO.deleteBook(selected.getBookId());
                    loadBooks();
                } catch (LibraryException ex) {
                    showError(ex.getMessage());
                }
            }
        });

        loadBooks();
    }

    private void loadBooks() {
        try {
            String search = searchField.getText();
            String sort = (String) sortBox.getSelectedItem();
            List<Book> books = bookDAO.getAllBooks(search, sort);
            tableModel.setRowCount(0);
            for (Book b : books) {
                tableModel.addRow(new Object[]{
                    b.getBookId(), b.getIsbn(), b.getTitle(), b.getAuthor(),
                    b.getGenre(), b.getTotalCopies(), b.getAvailableCopies()
                });
            }
        } catch (LibraryException ex) {
            showError(ex.getMessage());
        }
    }

    private Book getSelectedBook() {
        int row = table.getSelectedRow();
        if (row == -1) return null;
        row = table.convertRowIndexToModel(row);
        Book b = new Book();
        b.setBookId((int) tableModel.getValueAt(row, 0));
        b.setIsbn((String) tableModel.getValueAt(row, 1));
        b.setTitle((String) tableModel.getValueAt(row, 2));
        b.setAuthor((String) tableModel.getValueAt(row, 3));
        b.setGenre((String) tableModel.getValueAt(row, 4));
        b.setTotalCopies((int) tableModel.getValueAt(row, 5));
        b.setAvailableCopies((int) tableModel.getValueAt(row, 6));
        return b;
    }

    private void showBookDialog(Book existing) {
        JTextField isbnField = new JTextField(existing != null ? existing.getIsbn() : "");
        JTextField titleField = new JTextField(existing != null ? existing.getTitle() : "");
        JTextField authorField = new JTextField(existing != null ? existing.getAuthor() : "");
        JTextField genreField = new JTextField(existing != null ? existing.getGenre() : "");
        JTextField copiesField = new JTextField(existing != null ? String.valueOf(existing.getTotalCopies()) : "");

        JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
        panel.add(new JLabel("ISBN (10 or 13 digits):"));
        panel.add(isbnField);
        panel.add(new JLabel("Title:"));
        panel.add(titleField);
        panel.add(new JLabel("Author:"));
        panel.add(authorField);
        panel.add(new JLabel("Genre:"));
        panel.add(genreField);
        panel.add(new JLabel("Total Copies:"));
        panel.add(copiesField);

        int result = JOptionPane.showConfirmDialog(this, panel,
            existing == null ? "Add Book" : "Edit Book",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) return;

        try {
            int totalCopies;
            try {
                totalCopies = Integer.parseInt(copiesField.getText().trim());
            } catch (NumberFormatException nfe) {
                throw new LibraryException("Total Copies must be a whole number.");
            }

            if (existing == null) {
                Book book = new Book(isbnField.getText().trim(), titleField.getText().trim(),
                    authorField.getText().trim(), genreField.getText().trim(), totalCopies);
                bookDAO.addBook(book);
            } else {
                existing.setIsbn(isbnField.getText().trim());
                existing.setTitle(titleField.getText().trim());
                existing.setAuthor(authorField.getText().trim());
                existing.setGenre(genreField.getText().trim());
                // Keep available_copies consistent if total copies increased
                int delta = totalCopies - existing.getTotalCopies();
                existing.setTotalCopies(totalCopies);
                existing.setAvailableCopies(Math.max(0, existing.getAvailableCopies() + delta));
                bookDAO.updateBook(existing);
            }
            loadBooks();
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
