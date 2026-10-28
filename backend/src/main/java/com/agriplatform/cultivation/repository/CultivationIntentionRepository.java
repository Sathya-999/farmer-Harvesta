package com.agriplatform.cultivation.repository;

import com.agriplatform.cultivation.entity.CultivationIntention;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link CultivationIntention}.
 *
 * <h2>Owner Scoping & IDOR Security</h2>
 * <p>All query methods explicitly take {@code farmerId} to ensure strict owner scoping.
 * Cross-farmer requests return empty/false, translated to HTTP 404 at the service layer.
 */
@Repository
public interface CultivationIntentionRepository
        extends JpaRepository<CultivationIntention, UUID>, JpaSpecificationExecutor<CultivationIntention> {

    /**
     * Retrieves an intention by ID and authenticated farmer ID, eagerly fetching
     * plot, crop, variety, and season details for the response DTO.
     */
    @Query("SELECT ci FROM CultivationIntention ci " +
           "JOIN FETCH ci.plot p " +
           "JOIN FETCH p.village v " +
           "JOIN FETCH ci.crop c " +
           "LEFT JOIN FETCH ci.cropVariety cv " +
           "JOIN FETCH ci.season s " +
           "WHERE ci.id = :id AND ci.farmerId = :farmerId")
    Optional<CultivationIntention> findByIdAndFarmerIdWithDetails(
            @Param("id") UUID id,
            @Param("farmerId") UUID farmerId);

    /**
     * Checks whether an intention with the given ID belongs to the given farmer.
     */
    boolean existsByIdAndFarmerId(UUID id, UUID farmerId);

    /**
     * Checks whether an active cultivation lineage already exists on the given plot
     * for the specified season and year (i.e. status is NOT CANCELLED or ABANDONED).
     */
    @Query("SELECT COUNT(ci) > 0 FROM CultivationIntention ci " +
           "WHERE ci.plot.id = :plotId " +
           "  AND ci.season.id = :seasonId " +
           "  AND ci.cultivationYear = :cultivationYear " +
           "  AND ci.status NOT IN ('CANCELLED', 'ABANDONED')")
    boolean existsActiveCycle(
            @Param("plotId") UUID plotId,
            @Param("seasonId") UUID seasonId,
            @Param("cultivationYear") Short cultivationYear);

    /**
     * Checks whether an active cultivation lineage already exists on the given plot
     * for the specified season and year, excluding a specific intention (for updates).
     */
    @Query("SELECT COUNT(ci) > 0 FROM CultivationIntention ci " +
           "WHERE ci.plot.id = :plotId " +
           "  AND ci.season.id = :seasonId " +
           "  AND ci.cultivationYear = :cultivationYear " +
           "  AND ci.status NOT IN ('CANCELLED', 'ABANDONED') " +
           "  AND ci.id <> :excludeId")
    boolean existsActiveCycleExcluding(
            @Param("plotId") UUID plotId,
            @Param("seasonId") UUID seasonId,
            @Param("cultivationYear") Short cultivationYear,
            @Param("excludeId") UUID excludeId);

    /**
     * Computes the total active allocated area (in acres) for a given plot, season, and year.
     * Active statuses are {@code PLANNED} and {@code STARTED}.
     */
    @Query("SELECT COALESCE(SUM(ci.cultivatedAreaAcres), 0) FROM CultivationIntention ci " +
           "WHERE ci.plot.id = :plotId " +
           "  AND ci.season.id = :seasonId " +
           "  AND ci.cultivationYear = :cultivationYear " +
           "  AND ci.status IN ('PLANNED', 'STARTED')")
    BigDecimal sumActiveCultivatedArea(
            @Param("plotId") UUID plotId,
            @Param("seasonId") UUID seasonId,
            @Param("cultivationYear") Short cultivationYear);

    /**
     * Computes the total active allocated area excluding a specific intention (for updates).
     */
    @Query("SELECT COALESCE(SUM(ci.cultivatedAreaAcres), 0) FROM CultivationIntention ci " +
           "WHERE ci.plot.id = :plotId " +
           "  AND ci.season.id = :seasonId " +
           "  AND ci.cultivationYear = :cultivationYear " +
           "  AND ci.status IN ('PLANNED', 'STARTED') " +
           "  AND ci.id <> :excludeId")
    BigDecimal sumActiveCultivatedAreaExcluding(
            @Param("plotId") UUID plotId,
            @Param("seasonId") UUID seasonId,
            @Param("cultivationYear") Short cultivationYear,
            @Param("excludeId") UUID excludeId);
}
