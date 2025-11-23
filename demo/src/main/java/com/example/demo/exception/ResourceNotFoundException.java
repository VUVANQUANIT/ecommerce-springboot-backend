package com.example.demo.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resource, Long id) {
        super(String.format("%s với ID %d không tồn tại", resource, id));
    }

    public ResourceNotFoundException(String resource, String field, Object value) {
        super(String.format("%s với %s = %s không tồn tại", resource, field, value));
    }
}

