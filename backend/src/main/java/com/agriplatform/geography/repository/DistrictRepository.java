package com.agriplatform.geography.repository;

import com.agriplatform.geography.entity.District;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Read-only repository for {@link District} entities.
 *
 * <p>Provides access to the {@code public.districts} Supabase table.
 * No write operations are exposed — geography data is reference-only.
 */
@Repository
public interface DistrictRepository extends JpaRepository<District, UUID> {

    /**
     * Returns all active districts belonging to the given state, ordered by name.
     *
     * <p>Uses a JPQL query to select districts by the {@code state.id} FK
     * without triggering an eager join on the full State object.
     *
     * @param stateId the parent state UUID
     * @return list of active districts for the state, sorted alphabetically
     */
    @Query("SELECT d FROM District d WHERE d.state.id = :stateId AND d.isActive = true ORDER BY d.name ASC")
    List<District> findActiveByStateId(@Param("stateId") UUID stateId);

    @Query("SELECT d FROM District d WHERE d.state.id = :stateId AND LOWER(TRIM(d.name)) = LOWER(TRIM(:name))")
    java.util.Optional<District> findByStateIdAndNameIgnoreCase(@Param("stateId") UUID stateId, @Param("name") String name);

    @Query("SELECT d FROM District d WHERE d.state.id = :stateId AND LOWER(TRIM(d.code)) = LOWER(TRIM(:code))")
    java.util.Optional<District> findByStateIdAndCodeIgnoreCase(@Param("stateId") UUID stateId, @Param("code") String code);

    @Query("SELECT d FROM District d WHERE d.state.id = :stateId")
    List<District> findAllByStateId(@Param("stateId") UUID stateId);
}
