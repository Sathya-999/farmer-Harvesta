package com.agriplatform.cultivation.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA entity mapping the {@code public.cultivation_status_events} audit table in Supabase.
 *
 * <h2>System-Generated (Immutable)</h2>
 * <p>Rows are created automatically by DB triggers:
 * <ul>
 *   <li>{@code trg_record_status_event_insert} on INSERT</li>
 *   <li>{@code trg_record_status_event_update} on UPDATE when status changes</li>
 * </ul>
 * The application layer has READ-ONLY access to this table.
 */
@Entity
@Table(name = "cultivation_status_events", schema = "public")
public class CultivationStatusEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "intention_id", nullable = false, updatable = false)
    private UUID intentionId;

    @Column(name = "from_status", updatable = false)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, updatable = false)
    private String toStatus;

    @Column(name = "changed_by", updatable = false)
    private UUID changedBy;

    @Column(name = "actor_type", nullable = false, updatable = false)
    private String actorType;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private OffsetDateTime changedAt;

    @Column(name = "note", updatable = false)
    private String note;

    public CultivationStatusEvent() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getIntentionId() {
        return intentionId;
    }

    public void setIntentionId(UUID intentionId) {
        this.intentionId = intentionId;
    }

    public String getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(String fromStatus) {
        this.fromStatus = fromStatus;
    }

    public String getToStatus() {
        return toStatus;
    }

    public void setToStatus(String toStatus) {
        this.toStatus = toStatus;
    }

    public UUID getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(UUID changedBy) {
        this.changedBy = changedBy;
    }

    public String getActorType() {
        return actorType;
    }

    public void setActorType(String actorType) {
        this.actorType = actorType;
    }

    public OffsetDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(OffsetDateTime changedAt) {
        this.changedAt = changedAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
