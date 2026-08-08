-- ============================================================
-- Library Management System - Database Schema
-- TechnGlobal Java Minor Project
-- ============================================================

DROP DATABASE IF EXISTS library_management;
CREATE DATABASE library_management;
USE library_management;

-- ------------------------------------------------------------
-- Table: books
-- ------------------------------------------------------------
CREATE TABLE books (
    book_id         INT AUTO_INCREMENT PRIMARY KEY,
    isbn            VARCHAR(20)  NOT NULL UNIQUE,
    title           VARCHAR(200) NOT NULL,
    author          VARCHAR(150) NOT NULL,
    genre           VARCHAR(100),
    total_copies    INT          NOT NULL DEFAULT 1,
    available_copies INT         NOT NULL DEFAULT 1,
    added_date      DATE         NOT NULL DEFAULT (CURRENT_DATE),
    CHECK (available_copies >= 0 AND available_copies <= total_copies)
);

-- ------------------------------------------------------------
-- Table: members
-- ------------------------------------------------------------
CREATE TABLE members (
    member_id       INT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(150) NOT NULL,
    email           VARCHAR(150) NOT NULL UNIQUE,
    phone           VARCHAR(20),
    address         VARCHAR(255),
    membership_date DATE NOT NULL DEFAULT (CURRENT_DATE)
);

-- ------------------------------------------------------------
-- Table: transactions  (borrow / return records)
-- ------------------------------------------------------------
CREATE TABLE transactions (
    transaction_id  INT AUTO_INCREMENT PRIMARY KEY,
    book_id         INT NOT NULL,
    member_id       INT NOT NULL,
    issue_date      DATE NOT NULL,
    due_date        DATE NOT NULL,
    return_date     DATE NULL,
    fine_amount     DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    status          ENUM('ISSUED', 'RETURNED', 'OVERDUE') NOT NULL DEFAULT 'ISSUED',
    FOREIGN KEY (book_id) REFERENCES books(book_id) ON DELETE CASCADE,
    FOREIGN KEY (member_id) REFERENCES members(member_id) ON DELETE CASCADE
);

-- Helpful indexes for search/filter/sort features
CREATE INDEX idx_books_title ON books(title);
CREATE INDEX idx_books_author ON books(author);
CREATE INDEX idx_members_name ON members(name);
CREATE INDEX idx_transactions_status ON transactions(status);
