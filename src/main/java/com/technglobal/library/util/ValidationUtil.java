package com.technglobal.library.util;

import com.technglobal.library.exception.LibraryException;

import java.util.regex.Pattern;

/**
 * Centralized input validation so every form/DAO enforces the same rules.
 * Throws LibraryException with a human-readable message on failure —
 * the UI layer just needs to catch and display it.
 */
public final class ValidationUtil {

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$");

    private static final Pattern ISBN_PATTERN =
        Pattern.compile("^[0-9]{10}([0-9]{3})?$"); // ISBN-10 or ISBN-13, digits only

    private static final Pattern PHONE_PATTERN =
        Pattern.compile("^[0-9]{7,15}$");

    private ValidationUtil() {
    }

    public static void requireNonBlank(String value, String fieldName) throws LibraryException {
        if (value == null || value.trim().isEmpty()) {
            throw new LibraryException(fieldName + " cannot be empty.");
        }
    }

    public static void validateEmail(String email) throws LibraryException {
        requireNonBlank(email, "Email");
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new LibraryException("Invalid email format: " + email);
        }
    }

    public static void validateIsbn(String isbn) throws LibraryException {
        requireNonBlank(isbn, "ISBN");
        if (!ISBN_PATTERN.matcher(isbn.trim()).matches()) {
            throw new LibraryException("Invalid ISBN. Must be 10 or 13 digits.");
        }
    }

    public static void validatePhone(String phone) throws LibraryException {
        if (phone == null || phone.trim().isEmpty()) {
            return; // phone is optional
        }
        if (!PHONE_PATTERN.matcher(phone.trim()).matches()) {
            throw new LibraryException("Invalid phone number. Use 7-15 digits only.");
        }
    }

    public static void validatePositiveInt(int value, String fieldName) throws LibraryException {
        if (value <= 0) {
            throw new LibraryException(fieldName + " must be a positive number.");
        }
    }
}
