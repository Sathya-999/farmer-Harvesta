package com.agriplatform.cultivation.entity;

import com.agriplatform.plot.entity.Plot;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA entity mapping the {@code public.cultivation_intentions} table in Supabase.
 *
 * <h2>Schema (frozen)</h2>
 * <pre>
 *   id                    uuid        PK DEFAULT gen_random_uuid()
 *   plot_id               uuid        NOT NULL FK → plots(id) ON DELETE RESTRICT
 *   farmer_id             uuid        NOT NULL FK → farmer_profiles(id) ON DELETE RESTRICT
 *   crop_id               uuid        NOT NULL FK → crops(id) ON DELETE RESTRICT
 *   crop_variety_id       uuid        nullable FK → crop_varieties(id) ON DELETE RESTRICT
 *   season_id             uuid        NOT NULL FK → seasons(id) ON DELETE RESTRICT
 *   cultivation_year      smallint    NOT NULL CHECK (cultivation_year BETWEEN 2000 AND 2100)
 *   cultivated_area_acres numeric     NOT NULL CHECK (cultivated_area_acres > 0)
 *   planned_planting_date date        NOT NULL
 *   actual_planting_date  date        nullable
 *   actual_area_acres     numeric     nullable CHECK (actual_area_acres IS NULL OR actual_area_acres > 0)
 *   harvest_date          date        nullable CHECK (harvest_date IS NULL OR actual_planting_date IS NULL OR harvest_date >= actual_planting_date)
 *   status                text        NOT NULL DEFAULT 'PLANNED' CHECK (status IN ('PLANNED','STARTED','COMPLETED','CANCELLED','ABANDONED'))
 *   status_reason         text        nullable
 *   version               integer     NOT NULL DEFAULT 1 CHECK (version >= 1)
 *   source                text        NOT NULL DEFAULT 'FARMER_APP' CHECK (source IN ('FARMER_APP','ADMIN_ENTRY','IMPORT'))
 *   created_at            timestamptz NOT NULL DEFAULT now()
 *   updated_at            timestamptz NOT NULL DEFAULT now()
 * </pre>
 */
@Entity
@Table(name = "cultivation_intentions", schema = "public")
public class CultivationIntention {

    public static final String STATUS_PLANNED = "PLANNED";
    public static final String STATUS_STARTED = "STARTED";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_ABANDONED = "ABANDONED";

    public static final String SOURCE_FARMER_APP = "FARMER_APP";
    public static final String SOURCE_ADMIN_ENTRY = "ADMIN_ENTRY";
    public static final String SOURCE_IMPORT = "IMPORT";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plot_id", nullable = false, updatable = false)
    private Plot plot;

    @Column(name = "farmer_id", nullable = false, updatable = false)
    private UUID farmerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crop_id", nullable = false)
    private Crop crop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crop_variety_id")
    private CropVariety cropVariety;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id", nullable = false, updatable = false)
    private Season season;

    @Column(name = "cultivation_year", nullable = false, updatable = false)
    private Short cultivationYear;

    @Column(name = "cultivated_area_acres", nullable = false)
    private BigDecimal cultivatedAreaAcres;

    @Column(name = "planned_planting_date", nullable = false)
    private LocalDate plannedPlantingDate;

    @Column(name = "actual_planting_date")
    private LocalDate actualPlantingDate;

    @Column(name = "actual_area_acres")
    private BigDecimal actualAreaAcres;

    @Column(name = "harvest_date")
    private LocalDate harvestDate;

    @Column(name = "status", nullable = false)
    private String status = STATUS_PLANNED;

    @Column(name = "status_reason")
    private String statusReason;

    @Column(name = "version", nullable = false)
    private Integer version = 1;

    @Column(name = "source", nullable = false, updatable = false)
    private String source = SOURCE_FARMER_APP;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public CultivationIntention() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Plot getPlot() {
        return plot;
    }

    public void setPlot(Plot plot) {
        this.plot = plot;
    }

    public UUID getFarmerId() {
        return farmerId;
    }

    public void setFarmerId(UUID farmerId) {
        this.farmerId = farmerId;
    }

    public Crop getCrop() {
        return crop;
    }

    public void setCrop(Crop crop) {
        this.crop = crop;
    }

    public CropVariety getCropVariety() {
        return cropVariety;
    }

    public void setCropVariety(CropVariety cropVariety) {
        this.cropVariety = cropVariety;
    }

    public Season getSeason() {
        return season;
    }

    public void setSeason(Season season) {
        this.season = season;
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

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isTerminal() {
        return STATUS_COMPLETED.equals(status) ||
               STATUS_CANCELLED.equals(status) ||
               STATUS_ABANDONED.equals(status);
    }
}
