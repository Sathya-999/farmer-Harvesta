package com.agriplatform.common.exception;

import com.agriplatform.common.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for the Agricultural Intelligence Platform.
 *
 * <p>Converts well-known exceptions to standardised {@link ApiResponse} error
 * envelopes with appropriate HTTP status codes. All error responses follow the
 * same shape as success responses so clients can parse them uniformly.
 *
 * <h2>Handled exceptions</h2>
 * <ul>
 *   <li>{@link ResourceNotFoundException} → HTTP 404</li>
 *   <li>{@link Exception} (fallback) → HTTP 500</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles {@link ResourceNotFoundException} — returns HTTP 404.
     *
     * <p>Logs at WARN level (not ERROR) because 404s are expected client behaviour.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Handles {@link ConflictException} — returns HTTP 409.
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(ConflictException ex) {
        log.warn("Conflict: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Handles {@link org.springframework.dao.DataIntegrityViolationException} — returns HTTP 409.
     *
     * <p>Thrown when a live DB constraint fires after Java-layer validation:
     * e.g. duplicate plot label ({@code ux_plots_farmer_label_ci}),
     * area check ({@code plots_area_positive}), or idempotency key collision.
     * Raw SQL details are NOT returned to the client.
     */
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(
            org.springframework.dao.DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        String message = extractConstraintMessage(ex);
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(message));
    }

    /**
     * Extracts a user-friendly conflict message from a DB constraint violation.
     * Does not leak raw SQL or table names.
     */
    private String extractConstraintMessage(org.springframework.dao.DataIntegrityViolationException ex) {
        String cause = ex.getMostSpecificCause().getMessage();
        if (cause != null) {
            if (cause.contains("users_email_partial_key") || cause.contains("users_email_key")) {
                return "An account with this email address already exists. Please sign in instead.";
            }
            if (cause.contains("users_phone_key")) {
                return "An account with this phone number already exists. Please sign in instead.";
            }
            if (cause.contains("ux_plots_farmer_label_ci")) {
                return "A plot with this label already exists for this farmer.";
            }
            if (cause.contains("uq_idempotency_farmer_key")) {
                return "Idempotency key conflict. Please retry with the original request.";
            }
            if (cause.contains("plots_area_positive")) {
                return "Plot area must be greater than 0.";
            }
            if (cause.contains("plots_label_valid")) {
                return "Plot label must not be empty or blank.";
            }
            if (cause.contains("idempotency_keys")) {
                return "Idempotency key conflict. Please retry with the original request.";
            }
            if (cause.contains("ux_active_cultivation_cycle")) {
                return "An active cultivation cycle already exists for this plot, season, and year.";
            }
            if (cause.contains("cultivation_variety_matches_crop")) {
                return "The selected variety does not belong to the selected crop.";
            }
            if (cause.contains("cultivation_plot_owner_fk")) {
                return "Plot does not belong to the authenticated farmer.";
            }
            if (cause.contains("cultivation_area_positive")) {
                return "Cultivated area must be greater than 0.";
            }
            if (cause.contains("cultivation_year_valid")) {
                return "Cultivation year must be between 2000 and 2100.";
            }
            if (cause.contains("cultivation_harvest_after_planting")) {
                return "Harvest date must be on or after actual planting date.";
            }
            if (cause.contains("actual_area_positive")) {
                return "Actual area must be greater than 0.";
            }
            if (cause.contains("CULTIVATION_CYCLE_IDENTITY_IMMUTABLE_USE_NEW_INTENTION")) {
                return "Cultivation cycle identity (plot, season, year) is immutable.";
            }
            if (cause.contains("ILLEGAL_STATUS_TRANSITION")) {
                return "Invalid status transition for cultivation cycle.";
            }
            if (cause.contains("TERMINAL_INTENTION_IS_NOT_EDITABLE")) {
                return "Terminal cultivation cycle cannot be edited.";
            }
            if (cause.contains("ACTUAL_FIELDS_NOT_ALLOWED_FOR_STATUS")) {
                return "Actual planting date, actual area, and harvest date are not permitted for this status.";
            }
            if (cause.contains("HARVEST_DATE_REQUIRES_COMPLETED_STATUS")) {
                return "Harvest date is only permitted when status is COMPLETED.";
            }
            if (cause.contains("COMPLETED_REQUIRES_ACTUAL_PLANTING_DATE")) {
                return "Actual planting date is required when status is COMPLETED.";
            }
            if (cause.contains("INTENTION_ID_IMMUTABLE")) {
                return "Cultivation intention ID is immutable.";
            }
            if (cause.contains("INTENTION_CREATED_AT_IMMUTABLE")) {
                return "Cultivation intention creation timestamp is immutable.";
            }
            if (cause.contains("INTENTION_FARMER_IMMUTABLE")) {
                return "Cultivation intention farmer is immutable.";
            }
            if (cause.contains("INTENTION_SOURCE_IMMUTABLE")) {
                return "Cultivation intention source is immutable.";
            }
            if (cause.contains("INTENTION_FARMER_MUST_MATCH_PLOT_OWNER")) {
                return "Cultivation intention farmer must match plot owner.";
            }
            if (cause.contains("PLOT_INACTIVE")) {
                return "Cannot create cultivation on an inactive plot.";
            }
            if (cause.contains("CROP_INACTIVE")) {
                return "Selected crop is inactive.";
            }
            if (cause.contains("SEASON_INACTIVE")) {
                return "Selected season is inactive.";
            }
            if (cause.contains("INVALID_PLOT")) {
                return "Plot not found.";
            }
            if (cause.contains("INVALID_CROP")) {
                return "Crop not found.";
            }
            if (cause.contains("INVALID_SEASON")) {
                return "Season not found.";
            }
            if (cause.contains("ACTOR_CONTEXT_REQUIRED")) {
                return "Actor context is required for this operation.";
            }
        }
        return "Operation rejected due to a data conflict. Please check your request and retry.";
    }

    /**
     * Handles {@link BadRequestException} — returns HTTP 400.
     */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(BadRequestException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Handles {@link org.springframework.http.converter.HttpMessageNotReadableException} — returns HTTP 400.
     * Thrown when request JSON is malformed or unparseable.
     */
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMessageNotReadable(
            org.springframework.http.converter.HttpMessageNotReadableException ex) {
        log.warn("Malformed HTTP message / invalid JSON: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Malformed request payload. Please ensure the JSON structure is valid."));
    }

    /**
     * Handles {@link org.springframework.web.bind.MethodArgumentNotValidException} — returns HTTP 400.
     */
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(org.springframework.web.bind.MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .reduce((m1, m2) -> m1 + "; " + m2)
                .orElse("Validation failed");
        log.warn("Validation error: {}", errorMessage);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(errorMessage));
    }

    /**
     * Handles {@link org.springframework.security.authentication.BadCredentialsException} — returns HTTP 401.
     */
    @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(org.springframework.security.authentication.BadCredentialsException ex) {
        log.warn("Bad credentials: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Handles {@link com.agriplatform.common.util.SecurityUtils.AuthenticationRequiredException} — returns HTTP 401.
     */
    @ExceptionHandler(com.agriplatform.common.util.SecurityUtils.AuthenticationRequiredException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationRequired(com.agriplatform.common.util.SecurityUtils.AuthenticationRequiredException ex) {
        log.warn("Authentication required: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Handles {@link org.springframework.security.access.AccessDeniedException} — returns HTTP 403.
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(org.springframework.security.access.AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Access denied: " + ex.getMessage()));
    }

    /**
     * Handles {@link org.springframework.web.method.annotation.MethodArgumentTypeMismatchException} — returns HTTP 400.
     */
    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex) {
        log.warn("Method argument type mismatch: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Invalid parameter: " + ex.getName()));
    }

    /**
     * Handles {@link IllegalArgumentException} — returns HTTP 400.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Handles {@link org.springframework.web.HttpRequestMethodNotSupportedException} — returns HTTP 405.
     */
    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(org.springframework.web.HttpRequestMethodNotSupportedException ex) {
        log.warn("Method not supported: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Fallback handler for all unhandled exceptions — returns HTTP 500.
     *
     * <p>Logs the full stack trace. Returns a generic message to the client
     * to avoid leaking internal details.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred. Please try again later."));
    }
}
