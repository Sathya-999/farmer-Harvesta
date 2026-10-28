package com.agriplatform.plot.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA entity mapping the {@code public.idempotency_keys} table.
 *
 * <h2>Purpose</h2>
 * <p>Provides at-most-once semantics for Plot creation. When a client provides an
 * {@code Idempotency-Key} header on {@code POST /api/v1/plots}, the system:
 * <ol>
 *   <li>Checks whether a record with the same {@code (farmer_id, idempotency_key)} exists.</li>
 *   <li>If found with matching {@code request_hash}: returns the cached response.</li>
 *   <li>If found with different {@code request_hash}: rejects with 409 Conflict.</li>
 *   <li>If not found: inserts within the same transaction as the plot row.</li>
 * </ol>
 *
 * <h2>Schema</h2>
 * <pre>
 *   id               uuid        PK DEFAULT gen_random_uuid()
 *   farmer_id        uuid        NOT NULL FK → farmer_profiles(id) ON DELETE CASCADE
 *   idempotency_key  varchar(128) NOT NULL
 *   request_hash     varchar(64)  NOT NULL  (SHA-256 hex of method+uri+body)
 *   response_status  int          NOT NULL
 *   response_body    jsonb        NOT NULL
 *   created_at       timestamptz  NOT NULL DEFAULT now()
 *   expires_at       timestamptz  NOT NULL
 *   UNIQUE (farmer_id, idempotency_key)
 * </pre>
 */
@Entity
@Table(name = "idempotency_keys", schema = "public")
public class IdempotencyKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "farmer_id", nullable = false, updatable = false)
    private UUID farmerId;

    @Column(name = "idempotency_key", nullable = false, length = 128, updatable = false)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false, length = 64, updatable = false)
    private String requestHash;

    @Column(name = "response_status", nullable = false, updatable = false)
    private int responseStatus;

    /** Serialized {@code ApiResponse<PlotResponseDto>} JSON payload. */
    @Column(name = "response_body", nullable = false, columnDefinition = "jsonb", updatable = false)
    private String responseBody;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private OffsetDateTime expiresAt;

    public IdempotencyKey() {}

    public IdempotencyKey(UUID farmerId, String idempotencyKey, String requestHash,
                          int responseStatus, String responseBody, OffsetDateTime expiresAt) {
        this.farmerId = farmerId;
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.responseStatus = responseStatus;
        this.responseBody = responseBody;
        this.createdAt = OffsetDateTime.now();
        this.expiresAt = expiresAt;
    }

    public UUID getId() { return id; }
    public UUID getFarmerId() { return farmerId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getRequestHash() { return requestHash; }
    public int getResponseStatus() { return responseStatus; }
    public String getResponseBody() { return responseBody; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
}
