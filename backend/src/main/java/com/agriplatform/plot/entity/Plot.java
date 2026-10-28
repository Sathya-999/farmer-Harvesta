package com.agriplatform.plot.entity;

import com.agriplatform.geography.entity.Village;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA entity mapping the {@code public.plots} table in Supabase.
 *
 * <h2>Schema (frozen — do not modify)</h2>
 * <pre>
 *   id               uuid        PK DEFAULT gen_random_uuid()
 *   farmer_id        uuid        NOT NULL FK → farmer_profiles(id) ON DELETE RESTRICT
 *   village_id       uuid        NOT NULL FK → villages(id) ON DELETE RESTRICT
 *   label            text        nullable CHECK (label IS NULL OR btrim(label) <> '')
 *   total_area_acres numeric     nullable CHECK (total_area_acres IS NULL OR total_area_acres > 0)
 *   is_active        boolean     NOT NULL DEFAULT true
 *   created_at       timestamptz NOT NULL DEFAULT now()
 *   updated_at       timestamptz NOT NULL DEFAULT now()
 * </pre>
 *
 * <h2>Immutability</h2>
 * <ul>
 *   <li>{@code farmer_id} — immutable; enforced by DB trigger
 *       {@code PLOT_OWNER_TRANSFER_NOT_SUPPORTED_IN_ALPHA}</li>
 *   <li>{@code village_id} — immutable; enforced by DB trigger
 *       {@code PLOT_VILLAGE_IMMUTABLE_IN_ALPHA}</li>
 * </ul>
 *
 * <h2>Concurrency</h2>
 * <p>Optimistic concurrency uses {@code updated_at} as an opaque ISO-8601 microsecond-precision
 * string token. The DB trigger {@code trg_plots_updated_at} advances {@code updated_at}
 * on every UPDATE via {@code private.touch_updated_at()}.
 *
 * <h2>NO GPS columns</h2>
 * <p>Alpha V1 does not store plot GPS coordinates. Location is represented
 * by {@code village_id} only.
 */
@Entity
@Table(name = "plots", schema = "public")
public class Plot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /**
     * Plot owner. Derived from JWT {@code sub} claim; immutable after creation.
     * DB trigger prevents owner transfer.
     */
    @Column(name = "farmer_id", nullable = false, updatable = false)
    private UUID farmerId;

    /**
     * Village where the plot physically lies. Immutable after creation.
     * A farmer's home village and plot village may differ.
     * DB trigger prevents village_id change after INSERT.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "village_id", nullable = false, updatable = false)
    private Village village;

    /**
     * Optional human-readable label. Unique per farmer (case-insensitive, trimmed)
     * where not null. Enforced by partial unique index {@code ux_plots_farmer_label_ci}.
     */
    @Column(name = "label")
    private String label;

    /**
     * Total area in acres. Nullable (unknown area allowed in Alpha).
     * When supplied must be &gt; 0. Plain NUMERIC in live DB (no precision/scale restriction).
     */
    @Column(name = "total_area_acres")
    private BigDecimal totalAreaAcres;

    /**
     * Whether the plot is currently active. Soft-delete uses {@code false}.
     * Cannot be set to {@code false} when active cultivation intentions exist
     * (enforced by DB trigger {@code CANNOT_DEACTIVATE_PLOT_WITH_ACTIVE_CULTIVATION}).
     */
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /**
     * Touched by DB trigger {@code trg_plots_updated_at} on every UPDATE.
     * Stored at microsecond precision (timestamptz, datetime_precision=6).
     * Used as opaque concurrency version token.
     */
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public Plot() {}

    // -----------------------------------------------------------------------
    // Getters and setters
    // -----------------------------------------------------------------------

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getFarmerId() { return farmerId; }
    public void setFarmerId(UUID farmerId) { this.farmerId = farmerId; }

    public Village getVillage() { return village; }
    public void setVillage(Village village) { this.village = village; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public BigDecimal getTotalAreaAcres() { return totalAreaAcres; }
    public void setTotalAreaAcres(BigDecimal totalAreaAcres) { this.totalAreaAcres = totalAreaAcres; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    /**
     * Returns the opaque ISO-8601 microsecond-precision version token.
     * Clients must echo this verbatim as {@code expectedVersion} in update requests.
     */
    public String getVersion() {
        return updatedAt != null ? updatedAt.toString() : null;
    }
}
