package com.agriplatform.plot.repository;

import com.agriplatform.plot.entity.Plot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link Plot} entities.
 *
 * <h2>IDOR Protection</h2>
 * <p>Every query that accesses a specific plot by ID is scoped to the authenticated
 * farmer's UUID. Farmer A cannot retrieve or modify Farmer B's plot.
 *
 * <h2>Optimistic Concurrency</h2>
 * <p>Plot updates are performed via a native conditional UPDATE that includes
 * {@code updated_at} as an equality condition. Returns 1 if successful, 0 if
 * the version token is stale or the plot does not exist for this farmer.
 */
@Repository
public interface PlotRepository extends JpaRepository<Plot, UUID> {

    /**
     * Loads a plot by ID with village hierarchy eagerly fetched.
     * Scoped to the authenticated farmer (IDOR protection).
     *
     * @param id       plot UUID
     * @param farmerId authenticated farmer UUID from JWT sub
     * @return optional plot with loaded geography hierarchy
     */
    @Query("SELECT p FROM Plot p " +
           "JOIN FETCH p.village v " +
           "JOIN FETCH v.mandal m " +
           "JOIN FETCH m.district d " +
           "JOIN FETCH d.state s " +
           "WHERE p.id = :id AND p.farmerId = :farmerId")
    Optional<Plot> findByIdAndFarmerIdWithVillage(
            @Param("id") UUID id,
            @Param("farmerId") UUID farmerId);

    /**
     * Checks whether a plot with the given ID belongs to the given farmer.
     * Used for disambiguation in concurrency conflicts (determines 404 vs 409).
     *
     * @param id       plot UUID
     * @param farmerId authenticated farmer UUID
     * @return true if the plot exists and belongs to the farmer
     */
    boolean existsByIdAndFarmerId(UUID id, UUID farmerId);

    /**
     * Lists all ACTIVE plots for the given farmer (paginated).
     *
     * @param farmerId authenticated farmer UUID
     * @param pageable pagination/sort parameters
     * @return page of active plots
     */
    @Query("SELECT p FROM Plot p " +
           "JOIN FETCH p.village v " +
           "JOIN FETCH v.mandal m " +
           "JOIN FETCH m.district d " +
           "JOIN FETCH d.state s " +
           "WHERE p.farmerId = :farmerId AND p.isActive = true")
    Page<Plot> findActiveByFarmerId(@Param("farmerId") UUID farmerId, Pageable pageable);

    /**
     * Lists all plots for the given farmer regardless of active status (paginated).
     *
     * @param farmerId authenticated farmer UUID
     * @param pageable pagination/sort parameters
     * @return page of all plots
     */
    @Query("SELECT p FROM Plot p " +
           "JOIN FETCH p.village v " +
           "JOIN FETCH v.mandal m " +
           "JOIN FETCH m.district d " +
           "JOIN FETCH d.state s " +
           "WHERE p.farmerId = :farmerId")
    Page<Plot> findAllByFarmerId(@Param("farmerId") UUID farmerId, Pageable pageable);

    /**
     * Lists INACTIVE plots for the given farmer (paginated).
     *
     * @param farmerId authenticated farmer UUID
     * @param pageable pagination/sort parameters
     * @return page of inactive plots
     */
    @Query("SELECT p FROM Plot p " +
           "JOIN FETCH p.village v " +
           "JOIN FETCH v.mandal m " +
           "JOIN FETCH m.district d " +
           "JOIN FETCH d.state s " +
           "WHERE p.farmerId = :farmerId AND p.isActive = false")
    Page<Plot> findInactiveByFarmerId(@Param("farmerId") UUID farmerId, Pageable pageable);

    /**
     * Performs an atomic conditional UPDATE for optimistic concurrency control.
     *
     * <p>The {@code updated_at = :expectedVersion::timestamptz} condition is the
     * concurrency guard. If another client has already updated the plot, the
     * {@code updated_at} will differ and this query will return 0.
     *
     * <p>The DB trigger {@code trg_plots_updated_at} automatically touches
     * {@code updated_at := now()} on the committed row, advancing the version token.
     *
     * @param id              plot UUID
     * @param farmerId        authenticated farmer UUID (IDOR scope)
     * @param label           new label value (null = clear label)
     * @param totalAreaAcres  new area value (null = clear area)
     * @param expectedVersion opaque ISO-8601 microsecond-precision token from prior response
     * @return number of rows updated (1 = success, 0 = conflict or not found)
     */
    @Modifying
    @Query(nativeQuery = true,
           value = "UPDATE public.plots " +
                   "SET label = :label, " +
                   "    total_area_acres = :totalAreaAcres " +
                   "WHERE id = :id " +
                   "  AND farmer_id = :farmerId " +
                   "  AND updated_at = CAST(:expectedVersion AS timestamptz)")
    int conditionalUpdate(
            @Param("id") UUID id,
            @Param("farmerId") UUID farmerId,
            @Param("label") String label,
            @Param("totalAreaAcres") BigDecimal totalAreaAcres,
            @Param("expectedVersion") String expectedVersion);

    /**
     * Performs an atomic conditional soft-deactivation.
     *
     * <p>DB trigger {@code CANNOT_DEACTIVATE_PLOT_WITH_ACTIVE_CULTIVATION}
     * will reject the UPDATE if active cultivation exists.
     *
     * @param id       plot UUID
     * @param farmerId authenticated farmer UUID
     * @return rows updated (1 = success, 0 = not found or already inactive)
     */
    @Modifying
    @Query(nativeQuery = true,
           value = "UPDATE public.plots SET is_active = false " +
                   "WHERE id = :id AND farmer_id = :farmerId AND is_active = true")
    int deactivate(@Param("id") UUID id, @Param("farmerId") UUID farmerId);

    /**
     * Performs an atomic reactivation.
     *
     * @param id       plot UUID
     * @param farmerId authenticated farmer UUID
     * @return rows updated (1 = success, 0 = not found or already active)
     */
    @Modifying
    @Query(nativeQuery = true,
           value = "UPDATE public.plots SET is_active = true " +
                   "WHERE id = :id AND farmer_id = :farmerId AND is_active = false")
    int reactivate(@Param("id") UUID id, @Param("farmerId") UUID farmerId);

    /**
     * Returns the maximum committed cultivation area (in acres) for the given plot
     * across all ACTIVE intentions ({@code status IN ('PLANNED','STARTED')}).
     *
     * <p>Used to enforce the area reduction guard: the new area must not drop below
     * the maximum committed cultivation allocation in any active season/year.
     *
     * @param plotId   plot UUID
     * @param farmerId farmer UUID (IDOR scope via composite FK on cultivation_intentions)
     * @return maximum committed area, or null if no active intentions exist
     */
    @Query(nativeQuery = true,
           value = "SELECT MAX(sub.season_total) " +
                   "FROM (" +
                   "  SELECT SUM(ci.cultivated_area_acres) AS season_total " +
                   "  FROM public.cultivation_intentions ci " +
                   "  WHERE ci.plot_id = :plotId " +
                   "    AND ci.farmer_id = :farmerId " +
                   "    AND ci.status IN ('PLANNED', 'STARTED') " +
                   "  GROUP BY ci.season_id, ci.cultivation_year" +
                   ") sub")
    BigDecimal findMaxActiveCultivationArea(
            @Param("plotId") UUID plotId,
            @Param("farmerId") UUID farmerId);
}
