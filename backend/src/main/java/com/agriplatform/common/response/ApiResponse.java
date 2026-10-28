package com.agriplatform.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Standard API response envelope for the Agricultural Intelligence Platform.
 *
 * <h2>Structure</h2>
 * <pre>
 * {
 *   "success": true,
 *   "data": { ... },       // present on success
 *   "error": null,         // present on error
 *   "timestamp": "..."
 * }
 * </pre>
 *
 * <p>All controller endpoints wrap their payloads in this envelope to provide
 * a consistent, predictable response shape for API consumers.
 *
 * <p>{@code null} fields are excluded from JSON output via
 * {@link JsonInclude.Include#NON_NULL}.
 *
 * @param <T> type of the data payload
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        T data,
        String error,
        Instant timestamp
) {

    /**
     * Creates a successful response wrapping the given data.
     *
     * @param data the response payload
     * @param <T>  payload type
     * @return a successful {@link ApiResponse}
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, Instant.now());
    }

    /**
     * Creates an error response with the given message and no data payload.
     *
     * @param errorMessage human-readable error description
     * @param <T>          phantom payload type (always null for errors)
     * @return an error {@link ApiResponse}
     */
    public static <T> ApiResponse<T> error(String errorMessage) {
        return new ApiResponse<>(false, null, errorMessage, Instant.now());
    }
}
