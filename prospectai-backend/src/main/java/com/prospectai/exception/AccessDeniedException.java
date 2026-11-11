package com.prospectai.exception;

/**
 * Thrown when a user attempts an operation they are not authorized to perform.
 */
public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String message) {
        super(message);
    }
}
