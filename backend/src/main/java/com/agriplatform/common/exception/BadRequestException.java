package com.agriplatform.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when client input fails semantic or business validation rules
 * (e.g. invalid geography hierarchy, blank name, inactive reference entities).
 *
 * <p>Mapped to HTTP 400 Bad Request by {@link GlobalExceptionHandler}.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
