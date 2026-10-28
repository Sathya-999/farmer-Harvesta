package com.agriplatform.geography.repository;

import com.agriplatform.geography.entity.Village;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Read-only repository for {@link Village} entities.
 *
 * <p>Provides access to the {@code public.villages} Supabase table.
 * No write operations are exposed — geography data is reference-only.
 */
@Repository
public interface VillageRepository extends JpaRepository<Village, UUID> {

    /**
     * Returns all active villages belonging to the given mandal, ordered by name.
     *
     * @param mandalId the parent mandal UUID
     * @return list of active villages for the mandal, sorted alphabetically
     */
    @Query("SELECT v FROM Village v WHERE v.mandal.id = :mandalId AND v.isActive = true ORDER BY v.name ASC")
    List<Village> findActiveByMandalId(@Param("mandalId") UUID mandalId);

    @Query("SELECT v FROM Village v WHERE v.mandal.id = :mandalId AND LOWER(TRIM(v.name)) = LOWER(TRIM(:name))")
    java.util.Optional<Village> findByMandalIdAndNameIgnoreCase(@Param("mandalId") UUID mandalId, @Param("name") String name);

    @Query("SELECT v FROM Village v WHERE v.mandal.id = :mandalId AND LOWER(TRIM(v.code)) = LOWER(TRIM(:code))")
    java.util.Optional<Village> findByMandalIdAndCodeIgnoreCase(@Param("mandalId") UUID mandalId, @Param("code") String code);

    @Query("SELECT v FROM Village v WHERE v.mandal.id = :mandalId")
    List<Village> findAllByMandalId(@Param("mandalId") UUID mandalId);

    /**
     * Fetches a village along with its full hierarchy (Mandal, District, State) in a single query.
     *
     * @param id village UUID
     * @return optional containing the village with initialized hierarchy, or empty if not found
     */
    @Query("SELECT v FROM Village v " +
           "JOIN FETCH v.mandal m " +
           "JOIN FETCH m.district d " +
           "JOIN FETCH d.state s " +
           "WHERE v.id = :id")
    java.util.Optional<Village> findByIdWithHierarchy(@Param("id") UUID id);

    /**
     * Counts all villages belonging to a state in a single query.
     */
    @Query("SELECT COUNT(v) FROM Village v WHERE v.mandal.district.state.id = :stateId")
    long countByStateId(@Param("stateId") UUID stateId);

    /**
     * Retrieves active villages within a geographic bounding box with eager-fetched hierarchy.
     * Prevents unbounded full-table scans by prefiltering candidate coordinates at the database level.
     */
    @Query("SELECT v FROM Village v " +
           "JOIN FETCH v.mandal m " +
           "JOIN FETCH m.district d " +
           "JOIN FETCH d.state s " +
           "WHERE v.isActive = true " +
           "AND v.latitude BETWEEN :minLat AND :maxLat " +
           "AND v.longitude BETWEEN :minLon AND :maxLon")
    List<Village> findActiveWithinBoundingBox(
            @Param("minLat") java.math.BigDecimal minLat,
            @Param("maxLat") java.math.BigDecimal maxLat,
            @Param("minLon") java.math.BigDecimal minLon,
            @Param("maxLon") java.math.BigDecimal maxLon);
}

