package com.agriplatform.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a requested resource does not exist in the database.
 *
 * <p>Mapped to HTTP 404 Not Found by {@link GlobalExceptionHandler}.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 *   throw new ResourceNotFoundException("State not found: " + id);
 * }</pre>
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
