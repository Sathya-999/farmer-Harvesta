package com.agriplatform.notification.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Task 11 ? JPA entity mapping {@code public.notifications}.
 *
 * <h2>Schema (live ? do not modify without DB migration)</h2>
 * <pre>
 *   id         uuid        PK  DEFAULT gen_random_uuid()
 *   farmer_id  uuid        NOT NULL  FK ? farmer_profiles(id) ON DELETE RESTRICT
 *   title      text        NOT NULL  CHECK btrim(title) &lt;&gt; ''
 *   body       text        NOT NULL  CHECK btrim(body) &lt;&gt; ''
 *   type       text        NOT NULL  DEFAULT 'GENERAL'
 *   read_at    timestamptz NULL     (NULL = unread; non-null = read at that instant)
 *   created_at timestamptz NOT NULL DEFAULT now()
 * </pre>
 *
 * <h2>Read State</h2>
 * <p>Read state is tracked by {@code read_at}. A {@code null} value means the
 * notification is unread. Marking as read sets {@code read_at} to the current
 * timestamp once, atomically. The operation is idempotent: re-marking an
 * already-read notification is a no-op.
 *
 * <h2>No updated_at</h2>
 * <p>The live schema has no {@code updated_at} column. Concurrency on mark-read
 * is handled by a conditional UPDATE (WHERE read_at IS NULL).
 */
@Entity
@Table(name = "notifications", schema = "public")
public class Notification {

    public static final String TYPE_GENERAL       = "GENERAL";
    public static final String TYPE_CULTIVATION   = "CULTIVATION";
    public static final String TYPE_SYSTEM        = "SYSTEM";
    public static final String TYPE_ANALYTICS     = "ANALYTICS";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "farmer_id", nullable = false, updatable = false)
    private UUID farmerId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "body", nullable = false)
    private String body;

    @Column(name = "type", nullable = false)
    private String type = TYPE_GENERAL;

    @Column(name = "read_at")
    private OffsetDateTime readAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected Notification() {}

    public Notification(UUID farmerId, String title, String body, String type) {
        this.farmerId = farmerId;
        this.title = title;
        this.body = body;
        this.type = (type != null && !type.isBlank()) ? type : TYPE_GENERAL;
    }

    public UUID getId()                    { return id; }
    public UUID getFarmerId()              { return farmerId; }
    public String getTitle()               { return title; }
    public String getBody()               { return body; }
    public String getType()               { return type; }
    public OffsetDateTime getReadAt()      { return readAt; }
    public OffsetDateTime getCreatedAt()   { return createdAt; }

    public boolean isRead() { return readAt != null; }
}