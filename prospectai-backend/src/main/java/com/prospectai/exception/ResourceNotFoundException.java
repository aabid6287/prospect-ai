package com.prospectai.exception;

/**
 * Thrown when a requested resource does not exist or is not accessible.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
