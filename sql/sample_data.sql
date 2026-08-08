-- ============================================================
-- Sample data for testing (run after schema.sql)
-- ============================================================
USE library_management;

INSERT INTO books (isbn, title, author, genre, total_copies, available_copies) VALUES
('9780134685991', 'Effective Java', 'Joshua Bloch', 'Programming', 4, 4),
('9780596009205', 'Head First Design Patterns', 'Eric Freeman', 'Programming', 3, 3),
('9780132350884', 'Clean Code', 'Robert C. Martin', 'Programming', 5, 5),
('9780061120084', 'To Kill a Mockingbird', 'Harper Lee', 'Fiction', 2, 2),
('9780451524935', '1984', 'George Orwell', 'Fiction', 3, 3),
('9780262033848', 'Introduction to Algorithms', 'Thomas H. Cormen', 'Computer Science', 2, 2),
('9780743273565', 'The Great Gatsby', 'F. Scott Fitzgerald', 'Fiction', 4, 4),
('9781491910774', 'Designing Data-Intensive Applications', 'Martin Kleppmann', 'Technology', 2, 2);

INSERT INTO members (name, email, phone, address) VALUES
('Aarav Sharma', 'aarav.sharma@example.com', '9876500001', 'Hyderabad, Telangana'),
('Priya Reddy', 'priya.reddy@example.com', '9876500002', 'Secunderabad, Telangana'),
('Rohan Verma', 'rohan.verma@example.com', '9876500003', 'Warangal, Telangana'),
('Sneha Iyer', 'sneha.iyer@example.com', '9876500004', 'Vijayawada, AP');

-- A couple of sample transactions
INSERT INTO transactions (book_id, member_id, issue_date, due_date, return_date, fine_amount, status) VALUES
(1, 1, '2026-07-01', '2026-07-15', NULL, 0.00, 'ISSUED'),
(3, 2, '2026-06-20', '2026-07-04', '2026-07-02', 0.00, 'RETURNED');

-- Keep available_copies consistent with the ISSUED transaction above
UPDATE books SET available_copies = available_copies - 1 WHERE book_id = 1;
