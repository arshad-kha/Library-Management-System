package com.technglobal.library.exception;

/**
 * Custom checked exception for business-rule violations in the
 * Library Management System (e.g. no copies available, invalid input,
 * duplicate ISBN/email). Keeping this separate from raw SQLException
 * lets the UI layer show clean, user-friendly messages instead of
 * leaking database internals.
 */
public class LibraryException extends Exception {

    public LibraryException(String message) {
        super(message);
    }

    public LibraryException(String message, Throwable cause) {
        super(message, cause);
    }
}
