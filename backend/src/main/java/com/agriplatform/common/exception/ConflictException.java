package com.agriplatform.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when an update or initialization operation conflicts with current state,
 * such as an optimistic locking version mismatch or duplicate profile initialization.
 *
 * <p>Mapped to HTTP 409 Conflict by {@link GlobalExceptionHandler}.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
