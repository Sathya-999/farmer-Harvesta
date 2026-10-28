package com.agriplatform.cultivation.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA entity mapping the {@code public.cultivation_intention_history} immutable audit table in Supabase.
 *
 * <h2>System-Generated (Append-Only)</h2>
 * <p>Snapshots are automatically created by DB trigger {@code trg_record_intention_history}
 * whenever audited fields on {@code cultivation_intentions} are updated.
 * The application layer has READ-ONLY access to this table.
 */
@Entity
@Table(name = "cultivation_intention_history", schema = "public")
public class CultivationIntentionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "history_id", nullable = false, updatable = false)
    private UUID historyId;

    @Column(name = "intention_id", nullable = false, updatable = false)
    private UUID intentionId;

    @Column(name = "version", nullable = false, updatable = false)
    private Integer version;

    @Column(name = "snapshot_at", nullable = false, updatable = false)
    private OffsetDateTime snapshotAt;

    @Column(name = "plot_id", nullable = false, updatable = false)
    private UUID plotId;

    @Column(name = "farmer_id", nullable = false, updatable = false)
    private UUID farmerId;

    @Column(name = "crop_id", nullable = false, updatable = false)
    private UUID cropId;

    @Column(name = "crop_variety_id", updatable = false)
    private UUID cropVarietyId;

    @Column(name = "season_id", nullable = false, updatable = false)
    private UUID seasonId;

    @Column(name = "cultivation_year", nullable = false, updatable = false)
    private Short cultivationYear;

    @Column(name = "cultivated_area_acres", nullable = false, updatable = false)
    private BigDecimal cultivatedAreaAcres;

    @Column(name = "planned_planting_date", nullable = false, updatable = false)
    private LocalDate plannedPlantingDate;

    @Column(name = "actual_planting_date", updatable = false)
    private LocalDate actualPlantingDate;

    @Column(name = "actual_area_acres", updatable = false)
    private BigDecimal actualAreaAcres;

    @Column(name = "harvest_date", updatable = false)
    private LocalDate harvestDate;

    @Column(name = "status", nullable = false, updatable = false)
    private String status;

    @Column(name = "status_reason", updatable = false)
    private String statusReason;

    @Column(name = "source", nullable = false, updatable = false)
    private String source;

    @Column(name = "actor_id", updatable = false)
    private UUID actorId;

    @Column(name = "actor_type", nullable = false, updatable = false)
    private String actorType;

    @Column(name = "change_type", nullable = false, updatable = false)
    private String changeType;

    @Column(name = "changed_fields", columnDefinition = "text[]", updatable = false)
    private String[] changedFields;

    @Column(name = "change_reason", updatable = false)
    private String changeReason;

    public CultivationIntentionHistory() {
    }

    public UUID getHistoryId() {
        return historyId;
    }

    public void setHistoryId(UUID historyId) {
        this.historyId = historyId;
    }

    public UUID getIntentionId() {
        return intentionId;
    }

    public void setIntentionId(UUID intentionId) {
        this.intentionId = intentionId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public OffsetDateTime getSnapshotAt() {
        return snapshotAt;
    }

    public void setSnapshotAt(OffsetDateTime snapshotAt) {
        this.snapshotAt = snapshotAt;
    }

    public UUID getPlotId() {
        return plotId;
    }

    public void setPlotId(UUID plotId) {
        this.plotId = plotId;
    }

    public UUID getFarmerId() {
        return farmerId;
    }

    public void setFarmerId(UUID farmerId) {
        this.farmerId = farmerId;
    }

    public UUID getCropId() {
        return cropId;
    }

    public void setCropId(UUID cropId) {
        this.cropId = cropId;
    }

    public UUID getCropVarietyId() {
        return cropVarietyId;
    }

    public void setCropVarietyId(UUID cropVarietyId) {
        this.cropVarietyId = cropVarietyId;
    }

    public UUID getSeasonId() {
        return seasonId;
    }

    public void setSeasonId(UUID seasonId) {
        this.seasonId = seasonId;
    }

    public Short getCultivationYear() {
        return cultivationYear;
    }

    public void setCultivationYear(Short cultivationYear) {
        this.cultivationYear = cultivationYear;
    }

    public BigDecimal getCultivatedAreaAcres() {
        return cultivatedAreaAcres;
    }

    public void setCultivatedAreaAcres(BigDecimal cultivatedAreaAcres) {
        this.cultivatedAreaAcres = cultivatedAreaAcres;
    }

    public LocalDate getPlannedPlantingDate() {
        return plannedPlantingDate;
    }

    public void setPlannedPlantingDate(LocalDate plannedPlantingDate) {
        this.plannedPlantingDate = plannedPlantingDate;
    }

    public LocalDate getActualPlantingDate() {
        return actualPlantingDate;
    }

    public void setActualPlantingDate(LocalDate actualPlantingDate) {
        this.actualPlantingDate = actualPlantingDate;
    }

    public BigDecimal getActualAreaAcres() {
        return actualAreaAcres;
    }

    public void setActualAreaAcres(BigDecimal actualAreaAcres) {
        this.actualAreaAcres = actualAreaAcres;
    }

    public LocalDate getHarvestDate() {
        return harvestDate;
    }

    public void setHarvestDate(LocalDate harvestDate) {
        this.harvestDate = harvestDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public UUID getActorId() {
        return actorId;
    }

    public void setActorId(UUID actorId) {
        this.actorId = actorId;
    }

    public String getActorType() {
        return actorType;
    }

    public void setActorType(String actorType) {
        this.actorType = actorType;
    }

    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }

    public String[] getChangedFields() {
        return changedFields;
    }

    public void setChangedFields(String[] changedFields) {
        this.changedFields = changedFields;
    }

    public String getChangeReason() {
        return changeReason;
    }

    public void setChangeReason(String changeReason) {
        this.changeReason = changeReason;
    }
}
