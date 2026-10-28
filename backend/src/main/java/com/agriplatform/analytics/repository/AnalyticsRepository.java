package com.agriplatform.analytics.repository;

import com.agriplatform.cultivation.entity.CultivationIntention;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for high-performance database-side analytics aggregation.
 *
 * <h2>Performance & Index Utilization</h2>
 * <p>Queries are broken down by geographic level (Village, Mandal, District, State)
 * to avoid expensive OR-branching in PostgreSQL execution plans. Each query joins only
 * the minimum necessary tables along the geographic hierarchy:
 * <ul>
 *   <li>Village: joins {@code plots}, {@code farmer_profiles} via {@code idx_plots_village_id}</li>
 *   <li>Mandal: joins {@code plots}, {@code villages}, {@code farmer_profiles}</li>
 *   <li>District: joins {@code plots}, {@code villages}, {@code mandals}, {@code farmer_profiles}</li>
 *   <li>State: joins {@code plots}, {@code villages}, {@code mandals}, {@code districts}, {@code farmer_profiles}</li>
 * </ul>
 *
 * <h2>Domain Separation & Double-Counting Defense</h2>
 * <ul>
 *   <li>Only active farmer profiles ({@code profile_status = 'ACTIVE'}) contribute.</li>
 *   <li>Only active plots ({@code is_active = true}) contribute.</li>
 *   <li>Active metrics strictly filter {@code status IN ('PLANNED', 'STARTED')}.</li>
 *   <li>History snapshot tables are never queried for current state.</li>
 * </ul>
 */
@Repository
public interface AnalyticsRepository extends JpaRepository<CultivationIntention, UUID> {

    // =========================================================================
    // 1. GEOGRAPHIC SUMMARY AGGREGATION
    // =========================================================================

    @Query(value = """
        SELECT
            COALESCE(SUM(CASE WHEN ci.status IN ('PLANNED', 'STARTED') THEN ci.cultivated_area_acres ELSE 0 END), 0) AS activeArea,
            COUNT(CASE WHEN ci.status IN ('PLANNED', 'STARTED') THEN ci.id END) AS activeCultivations,
            COUNT(DISTINCT CASE WHEN ci.status IN ('PLANNED', 'STARTED') THEN ci.farmer_id END) AS activeFarmers,
            COUNT(DISTINCT CASE WHEN ci.status IN ('PLANNED', 'STARTED') THEN ci.plot_id END) AS activePlots,
            COALESCE(SUM(CASE WHEN ci.status = 'COMPLETED' THEN COALESCE(ci.actual_area_acres, ci.cultivated_area_acres) ELSE 0 END), 0) AS completedArea,
            COUNT(CASE WHEN ci.status = 'COMPLETED' THEN ci.id END) AS completedCultivations,
            COUNT(CASE WHEN ci.status = 'CANCELLED' THEN ci.id END) AS cancelledCultivations,
            COUNT(CASE WHEN ci.status = 'ABANDONED' THEN ci.id END) AS abandonedCultivations
        FROM public.cultivation_intentions ci
        JOIN public.plots p ON ci.plot_id = p.id
        JOIN public.farmer_profiles fp ON ci.farmer_id = fp.id
        WHERE p.village_id = :villageId
          AND p.is_active = true
          AND fp.profile_status = 'ACTIVE'
          AND (cast(:seasonId as uuid) IS NULL OR ci.season_id = cast(:seasonId as uuid))
          AND (cast(:cultivationYear as smallint) IS NULL OR ci.cultivation_year = cast(:cultivationYear as smallint))
        """, nativeQuery = true)
    GeographicSummaryProjection aggregateVillageSummary(
            @Param("villageId") UUID villageId,
            @Param("seasonId") UUID seasonId,
            @Param("cultivationYear") Short cultivationYear);

    @Query(value = """
        SELECT
            COALESCE(SUM(CASE WHEN ci.status IN ('PLANNED', 'STARTED') THEN ci.cultivated_area_acres ELSE 0 END), 0) AS activeArea,
            COUNT(CASE WHEN ci.status IN ('PLANNED', 'STARTED') THEN ci.id END) AS activeCultivations,
            COUNT(DISTINCT CASE WHEN ci.status IN ('PLANNED', 'STARTED') THEN ci.farmer_id END) AS activeFarmers,
            COUNT(DISTINCT CASE WHEN ci.status IN ('PLANNED', 'STARTED') THEN ci.plot_id END) AS activePlots,
            COALESCE(SUM(CASE WHEN ci.status = 'COMPLETED' THEN COALESCE(ci.actual_area_acres, ci.cultivated_area_acres) ELSE 0 END), 0) AS completedArea,
            COUNT(CASE WHEN ci.status = 'COMPLETED' THEN ci.id END) AS completedCultivations,
            COUNT(CASE WHEN ci.status = 'CANCELLED' THEN ci.id END) AS cancelledCultivations,
            COUNT(CASE WHEN ci.status = 'ABANDONED' THEN ci.id END) AS abandonedCultivations
        FROM public.cultivation_intentions ci
        JOIN public.plots p ON ci.plot_id = p.id
        JOIN public.villages v ON p.village_id = v.id
        JOIN public.farmer_profiles fp ON ci.farmer_id = fp.id
        WHERE v.mandal_id = :mandalId
          AND p.is_active = true
          AND fp.profile_status = 'ACTIVE'
          AND (cast(:seasonId as uuid) IS NULL OR ci.season_id = cast(:seasonId as uuid))
          AND (cast(:cultivationYear as smallint) IS NULL OR ci.cultivation_year = cast(:cultivationYear as smallint))
        """, nativeQuery = true)
    GeographicSummaryProjection aggregateMandalSummary(
            @Param("mandalId") UUID mandalId,
            @Param("seasonId") UUID seasonId,
            @Param("cultivationYear") Short cultivationYear);

    @Query(value = """
        SELECT
            COALESCE(SUM(CASE WHEN ci.status IN ('PLANNED', 'STARTED') THEN ci.cultivated_area_acres ELSE 0 END), 0) AS activeArea,
            COUNT(CASE WHEN ci.status IN ('PLANNED', 'STARTED') THEN ci.id END) AS activeCultivations,
            COUNT(DISTINCT CASE WHEN ci.status IN ('PLANNED', 'STARTED') THEN ci.farmer_id END) AS activeFarmers,
            COUNT(DISTINCT CASE WHEN ci.status IN ('PLANNED', 'STARTED') THEN ci.plot_id END) AS activePlots,
            COALESCE(SUM(CASE WHEN ci.status = 'COMPLETED' THEN COALESCE(ci.actual_area_acres, ci.cultivated_area_acres) ELSE 0 END), 0) AS completedArea,
            COUNT(CASE WHEN ci.status = 'COMPLETED' THEN ci.id END) AS completedCultivations,
            COUNT(CASE WHEN ci.status = 'CANCELLED' THEN ci.id END) AS cancelledCultivations,
            COUNT(CASE WHEN ci.status = 'ABANDONED' THEN ci.id END) AS abandonedCultivations
        FROM public.cultivation_intentions ci
        JOIN public.plots p ON ci.plot_id = p.id
        JOIN public.villages v ON p.village_id = v.id
        JOIN public.mandals m ON v.mandal_id = m.id
        JOIN public.farmer_profiles fp ON ci.farmer_id = fp.id
        WHERE m.district_id = :districtId
          AND p.is_active = true
          AND fp.profile_status = 'ACTIVE'
          AND (cast(:seasonId as uuid) IS NULL OR ci.season_id = cast(:seasonId as uuid))
          AND (cast(:cultivationYear as smallint) IS NULL OR ci.cultivation_year = cast(:cultivationYear as smallint))
        """, nativeQuery = true)
    GeographicSummaryProjection aggregateDistrictSummary(
            @Param("districtId") UUID districtId,
            @Param("seasonId") UUID seasonId,
            @Param("cultivationYear") Short cultivationYear);

    @Query(value = """
        SELECT
            COALESCE(SUM(CASE WHEN ci.status IN ('PLANNED', 'STARTED') THEN ci.cultivated_area_acres ELSE 0 END), 0) AS activeArea,
            COUNT(CASE WHEN ci.status IN ('PLANNED', 'STARTED') THEN ci.id END) AS activeCultivations,
            COUNT(DISTINCT CASE WHEN ci.status IN ('PLANNED', 'STARTED') THEN ci.farmer_id END) AS activeFarmers,
            COUNT(DISTINCT CASE WHEN ci.status IN ('PLANNED', 'STARTED') THEN ci.plot_id END) AS activePlots,
            COALESCE(SUM(CASE WHEN ci.status = 'COMPLETED' THEN COALESCE(ci.actual_area_acres, ci.cultivated_area_acres) ELSE 0 END), 0) AS completedArea,
            COUNT(CASE WHEN ci.status = 'COMPLETED' THEN ci.id END) AS completedCultivations,
            COUNT(CASE WHEN ci.status = 'CANCELLED' THEN ci.id END) AS cancelledCultivations,
            COUNT(CASE WHEN ci.status = 'ABANDONED' THEN ci.id END) AS abandonedCultivations
        FROM public.cultivation_intentions ci
        JOIN public.plots p ON ci.plot_id = p.id
        JOIN public.villages v ON p.village_id = v.id
        JOIN public.mandals m ON v.mandal_id = m.id
        JOIN public.districts d ON m.district_id = d.id
        JOIN public.farmer_profiles fp ON ci.farmer_id = fp.id
        WHERE d.state_id = :stateId
          AND p.is_active = true
          AND fp.profile_status = 'ACTIVE'
          AND (cast(:seasonId as uuid) IS NULL OR ci.season_id = cast(:seasonId as uuid))
          AND (cast(:cultivationYear as smallint) IS NULL OR ci.cultivation_year = cast(:cultivationYear as smallint))
        """, nativeQuery = true)
    GeographicSummaryProjection aggregateStateSummary(
            @Param("stateId") UUID stateId,
            @Param("seasonId") UUID seasonId,
            @Param("cultivationYear") Short cultivationYear);

    // =========================================================================
    // 2. CROP DISTRIBUTION AGGREGATION
    // =========================================================================

    @Query(value = """
        SELECT
            c.id AS cropId,
            c.name AS cropName,
            c.category AS cropCategory,
            COALESCE(SUM(ci.cultivated_area_acres), 0) AS totalArea,
            COUNT(ci.id) AS cultivationCount,
            COUNT(DISTINCT ci.farmer_id) AS farmerCount
        FROM public.cultivation_intentions ci
        JOIN public.crops c ON ci.crop_id = c.id
        JOIN public.plots p ON ci.plot_id = p.id
        JOIN public.farmer_profiles fp ON ci.farmer_id = fp.id
        WHERE p.village_id = :villageId
          AND p.is_active = true
          AND fp.profile_status = 'ACTIVE'
          AND ci.status IN ('PLANNED', 'STARTED')
          AND (cast(:seasonId as uuid) IS NULL OR ci.season_id = cast(:seasonId as uuid))
          AND (cast(:cultivationYear as smallint) IS NULL OR ci.cultivation_year = cast(:cultivationYear as smallint))
        GROUP BY c.id, c.name, c.category
        ORDER BY totalArea DESC, c.name ASC
        """, nativeQuery = true)
    List<CropDistributionProjection> findCropDistributionForVillage(
            @Param("villageId") UUID villageId,
            @Param("seasonId") UUID seasonId,
            @Param("cultivationYear") Short cultivationYear);

    @Query(value = """
        SELECT
            c.id AS cropId,
            c.name AS cropName,
            c.category AS cropCategory,
            COALESCE(SUM(ci.cultivated_area_acres), 0) AS totalArea,
            COUNT(ci.id) AS cultivationCount,
            COUNT(DISTINCT ci.farmer_id) AS farmerCount
        FROM public.cultivation_intentions ci
        JOIN public.crops c ON ci.crop_id = c.id
        JOIN public.plots p ON ci.plot_id = p.id
        JOIN public.villages v ON p.village_id = v.id
        JOIN public.farmer_profiles fp ON ci.farmer_id = fp.id
        WHERE v.mandal_id = :mandalId
          AND p.is_active = true
          AND fp.profile_status = 'ACTIVE'
          AND ci.status IN ('PLANNED', 'STARTED')
          AND (cast(:seasonId as uuid) IS NULL OR ci.season_id = cast(:seasonId as uuid))
          AND (cast(:cultivationYear as smallint) IS NULL OR ci.cultivation_year = cast(:cultivationYear as smallint))
        GROUP BY c.id, c.name, c.category
        ORDER BY totalArea DESC, c.name ASC
        """, nativeQuery = true)
    List<CropDistributionProjection> findCropDistributionForMandal(
            @Param("mandalId") UUID mandalId,
            @Param("seasonId") UUID seasonId,
            @Param("cultivationYear") Short cultivationYear);

    @Query(value = """
        SELECT
            c.id AS cropId,
            c.name AS cropName,
            c.category AS cropCategory,
            COALESCE(SUM(ci.cultivated_area_acres), 0) AS totalArea,
            COUNT(ci.id) AS cultivationCount,
            COUNT(DISTINCT ci.farmer_id) AS farmerCount
        FROM public.cultivation_intentions ci
        JOIN public.crops c ON ci.crop_id = c.id
        JOIN public.plots p ON ci.plot_id = p.id
        JOIN public.villages v ON p.village_id = v.id
        JOIN public.mandals m ON v.mandal_id = m.id
        JOIN public.farmer_profiles fp ON ci.farmer_id = fp.id
        WHERE m.district_id = :districtId
          AND p.is_active = true
          AND fp.profile_status = 'ACTIVE'
          AND ci.status IN ('PLANNED', 'STARTED')
          AND (cast(:seasonId as uuid) IS NULL OR ci.season_id = cast(:seasonId as uuid))
          AND (cast(:cultivationYear as smallint) IS NULL OR ci.cultivation_year = cast(:cultivationYear as smallint))
        GROUP BY c.id, c.name, c.category
        ORDER BY totalArea DESC, c.name ASC
        """, nativeQuery = true)
    List<CropDistributionProjection> findCropDistributionForDistrict(
            @Param("districtId") UUID districtId,
            @Param("seasonId") UUID seasonId,
            @Param("cultivationYear") Short cultivationYear);

    @Query(value = """
        SELECT
            c.id AS cropId,
            c.name AS cropName,
            c.category AS cropCategory,
            COALESCE(SUM(ci.cultivated_area_acres), 0) AS totalArea,
            COUNT(ci.id) AS cultivationCount,
            COUNT(DISTINCT ci.farmer_id) AS farmerCount
        FROM public.cultivation_intentions ci
        JOIN public.crops c ON ci.crop_id = c.id
        JOIN public.plots p ON ci.plot_id = p.id
        JOIN public.villages v ON p.village_id = v.id
        JOIN public.mandals m ON v.mandal_id = m.id
        JOIN public.districts d ON m.district_id = d.id
        JOIN public.farmer_profiles fp ON ci.farmer_id = fp.id
        WHERE d.state_id = :stateId
          AND p.is_active = true
          AND fp.profile_status = 'ACTIVE'
          AND ci.status IN ('PLANNED', 'STARTED')
          AND (cast(:seasonId as uuid) IS NULL OR ci.season_id = cast(:seasonId as uuid))
          AND (cast(:cultivationYear as smallint) IS NULL OR ci.cultivation_year = cast(:cultivationYear as smallint))
        GROUP BY c.id, c.name, c.category
        ORDER BY totalArea DESC, c.name ASC
        """, nativeQuery = true)
    List<CropDistributionProjection> findCropDistributionForState(
            @Param("stateId") UUID stateId,
            @Param("seasonId") UUID seasonId,
            @Param("cultivationYear") Short cultivationYear);

    // =========================================================================
    // 3. TEMPORAL TREND AGGREGATION
    // =========================================================================

    @Query(value = """
        SELECT
            ci.cultivation_year AS cultivationYear,
            s.id AS seasonId,
            s.name AS seasonName,
            COALESCE(SUM(CASE WHEN ci.status IN ('PLANNED', 'STARTED') THEN ci.cultivated_area_acres ELSE 0 END), 0) AS activeArea,
            COALESCE(SUM(CASE WHEN ci.status = 'COMPLETED' THEN COALESCE(ci.actual_area_acres, ci.cultivated_area_acres) ELSE 0 END), 0) AS completedArea,
            COUNT(ci.id) AS totalCultivations,
            COUNT(DISTINCT ci.farmer_id) AS totalFarmers
        FROM public.cultivation_intentions ci
        JOIN public.seasons s ON ci.season_id = s.id
        JOIN public.plots p ON ci.plot_id = p.id
        JOIN public.farmer_profiles fp ON ci.farmer_id = fp.id
        WHERE p.village_id = :villageId
          AND p.is_active = true
          AND fp.profile_status = 'ACTIVE'
          AND (cast(:cropId as uuid) IS NULL OR ci.crop_id = cast(:cropId as uuid))
          AND (cast(:startYear as smallint) IS NULL OR ci.cultivation_year >= cast(:startYear as smallint))
          AND (cast(:endYear as smallint) IS NULL OR ci.cultivation_year <= cast(:endYear as smallint))
        GROUP BY ci.cultivation_year, s.id, s.name
        ORDER BY ci.cultivation_year ASC, s.name ASC
        """, nativeQuery = true)
    List<TemporalTrendProjection> findTemporalTrendsForVillage(
            @Param("villageId") UUID villageId,
            @Param("cropId") UUID cropId,
            @Param("startYear") Short startYear,
            @Param("endYear") Short endYear);

    @Query(value = """
        SELECT
            ci.cultivation_year AS cultivationYear,
            s.id AS seasonId,
            s.name AS seasonName,
            COALESCE(SUM(CASE WHEN ci.status IN ('PLANNED', 'STARTED') THEN ci.cultivated_area_acres ELSE 0 END), 0) AS activeArea,
            COALESCE(SUM(CASE WHEN ci.status = 'COMPLETED' THEN COALESCE(ci.actual_area_acres, ci.cultivated_area_acres) ELSE 0 END), 0) AS completedArea,
            COUNT(ci.id) AS totalCultivations,
            COUNT(DISTINCT ci.farmer_id) AS totalFarmers
        FROM public.cultivation_intentions ci
        JOIN public.seasons s ON ci.season_id = s.id
        JOIN public.plots p ON ci.plot_id = p.id
        JOIN public.villages v ON p.village_id = v.id
        JOIN public.farmer_profiles fp ON ci.farmer_id = fp.id
        WHERE v.mandal_id = :mandalId
          AND p.is_active = true
          AND fp.profile_status = 'ACTIVE'
          AND (cast(:cropId as uuid) IS NULL OR ci.crop_id = cast(:cropId as uuid))
          AND (cast(:startYear as smallint) IS NULL OR ci.cultivation_year >= cast(:startYear as smallint))
          AND (cast(:endYear as smallint) IS NULL OR ci.cultivation_year <= cast(:endYear as smallint))
        GROUP BY ci.cultivation_year, s.id, s.name
        ORDER BY ci.cultivation_year ASC, s.name ASC
        """, nativeQuery = true)
    List<TemporalTrendProjection> findTemporalTrendsForMandal(
            @Param("mandalId") UUID mandalId,
            @Param("cropId") UUID cropId,
            @Param("startYear") Short startYear,
            @Param("endYear") Short endYear);

    @Query(value = """
        SELECT
            ci.cultivation_year AS cultivationYear,
            s.id AS seasonId,
            s.name AS seasonName,
            COALESCE(SUM(CASE WHEN ci.status IN ('PLANNED', 'STARTED') THEN ci.cultivated_area_acres ELSE 0 END), 0) AS activeArea,
            COALESCE(SUM(CASE WHEN ci.status = 'COMPLETED' THEN COALESCE(ci.actual_area_acres, ci.cultivated_area_acres) ELSE 0 END), 0) AS completedArea,
            COUNT(ci.id) AS totalCultivations,
            COUNT(DISTINCT ci.farmer_id) AS totalFarmers
        FROM public.cultivation_intentions ci
        JOIN public.seasons s ON ci.season_id = s.id
        JOIN public.plots p ON ci.plot_id = p.id
        JOIN public.villages v ON p.village_id = v.id
        JOIN public.mandals m ON v.mandal_id = m.id
        JOIN public.farmer_profiles fp ON ci.farmer_id = fp.id
        WHERE m.district_id = :districtId
          AND p.is_active = true
          AND fp.profile_status = 'ACTIVE'
          AND (cast(:cropId as uuid) IS NULL OR ci.crop_id = cast(:cropId as uuid))
          AND (cast(:startYear as smallint) IS NULL OR ci.cultivation_year >= cast(:startYear as smallint))
          AND (cast(:endYear as smallint) IS NULL OR ci.cultivation_year <= cast(:endYear as smallint))
        GROUP BY ci.cultivation_year, s.id, s.name
        ORDER BY ci.cultivation_year ASC, s.name ASC
        """, nativeQuery = true)
    List<TemporalTrendProjection> findTemporalTrendsForDistrict(
            @Param("districtId") UUID districtId,
            @Param("cropId") UUID cropId,
            @Param("startYear") Short startYear,
            @Param("endYear") Short endYear);

    @Query(value = """
        SELECT
            ci.cultivation_year AS cultivationYear,
            s.id AS seasonId,
            s.name AS seasonName,
            COALESCE(SUM(CASE WHEN ci.status IN ('PLANNED', 'STARTED') THEN ci.cultivated_area_acres ELSE 0 END), 0) AS activeArea,
            COALESCE(SUM(CASE WHEN ci.status = 'COMPLETED' THEN COALESCE(ci.actual_area_acres, ci.cultivated_area_acres) ELSE 0 END), 0) AS completedArea,
            COUNT(ci.id) AS totalCultivations,
            COUNT(DISTINCT ci.farmer_id) AS totalFarmers
        FROM public.cultivation_intentions ci
        JOIN public.seasons s ON ci.season_id = s.id
        JOIN public.plots p ON ci.plot_id = p.id
        JOIN public.villages v ON p.village_id = v.id
        JOIN public.mandals m ON v.mandal_id = m.id
        JOIN public.districts d ON m.district_id = d.id
        JOIN public.farmer_profiles fp ON ci.farmer_id = fp.id
        WHERE d.state_id = :stateId
          AND p.is_active = true
          AND fp.profile_status = 'ACTIVE'
          AND (cast(:cropId as uuid) IS NULL OR ci.crop_id = cast(:cropId as uuid))
          AND (cast(:startYear as smallint) IS NULL OR ci.cultivation_year >= cast(:startYear as smallint))
          AND (cast(:endYear as smallint) IS NULL OR ci.cultivation_year <= cast(:endYear as smallint))
        GROUP BY ci.cultivation_year, s.id, s.name
        ORDER BY ci.cultivation_year ASC, s.name ASC
        """, nativeQuery = true)
    List<TemporalTrendProjection> findTemporalTrendsForState(
            @Param("stateId") UUID stateId,
            @Param("cropId") UUID cropId,
            @Param("startYear") Short startYear,
            @Param("endYear") Short endYear);

    // =========================================================================
    // 4. FARMER PERSONAL ANALYTICS AGGREGATION (IDOR-PROTECTED)
    // =========================================================================

    @Query(value = """
        SELECT
            COUNT(p.id) AS totalPlots,
            COALESCE(SUM(p.total_area_acres), 0) AS totalLandCapacity
        FROM public.plots p
        WHERE p.farmer_id = :farmerId
          AND p.is_active = true
        """, nativeQuery = true)
    FarmerPlotSummaryProjection aggregateFarmerPlots(@Param("farmerId") UUID farmerId);

    @Query(value = """
        SELECT
            COALESCE(SUM(CASE WHEN ci.status IN ('PLANNED', 'STARTED') THEN ci.cultivated_area_acres ELSE 0 END), 0) AS activeArea,
            COALESCE(SUM(CASE WHEN ci.status = 'COMPLETED' THEN COALESCE(ci.actual_area_acres, ci.cultivated_area_acres) ELSE 0 END), 0) AS completedArea,
            COUNT(CASE WHEN ci.status IN ('PLANNED', 'STARTED') THEN ci.id END) AS activeCount,
            COUNT(CASE WHEN ci.status = 'COMPLETED' THEN ci.id END) AS completedCount,
            COUNT(CASE WHEN ci.status = 'CANCELLED' THEN ci.id END) AS cancelledCount,
            COUNT(CASE WHEN ci.status = 'ABANDONED' THEN ci.id END) AS abandonedCount
        FROM public.cultivation_intentions ci
        JOIN public.plots p ON ci.plot_id = p.id
        WHERE ci.farmer_id = :farmerId
          AND p.is_active = true
          AND (cast(:seasonId as uuid) IS NULL OR ci.season_id = cast(:seasonId as uuid))
          AND (cast(:cultivationYear as smallint) IS NULL OR ci.cultivation_year = cast(:cultivationYear as smallint))
        """, nativeQuery = true)
    FarmerCultivationSummaryProjection aggregateFarmerCultivation(
            @Param("farmerId") UUID farmerId,
            @Param("seasonId") UUID seasonId,
            @Param("cultivationYear") Short cultivationYear);

    @Query(value = """
        SELECT
            c.id AS cropId,
            c.name AS cropName,
            c.category AS cropCategory,
            COALESCE(SUM(ci.cultivated_area_acres), 0) AS activeArea,
            COUNT(ci.id) AS cultivationCyclesCount
        FROM public.cultivation_intentions ci
        JOIN public.crops c ON ci.crop_id = c.id
        JOIN public.plots p ON ci.plot_id = p.id
        WHERE ci.farmer_id = :farmerId
          AND p.is_active = true
          AND ci.status IN ('PLANNED', 'STARTED')
          AND (cast(:seasonId as uuid) IS NULL OR ci.season_id = cast(:seasonId as uuid))
          AND (cast(:cultivationYear as smallint) IS NULL OR ci.cultivation_year = cast(:cultivationYear as smallint))
        GROUP BY c.id, c.name, c.category
        ORDER BY activeArea DESC, c.name ASC
        """, nativeQuery = true)
    List<FarmerCropBreakdownProjection> findFarmerCropBreakdown(
            @Param("farmerId") UUID farmerId,
            @Param("seasonId") UUID seasonId,
            @Param("cultivationYear") Short cultivationYear);
}
