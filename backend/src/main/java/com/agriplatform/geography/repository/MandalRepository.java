package com.agriplatform.geography.repository;

import com.agriplatform.geography.entity.Mandal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Read-only repository for {@link Mandal} entities.
 *
 * <p>Provides access to the {@code public.mandals} Supabase table.
 * No write operations are exposed — geography data is reference-only.
 */
@Repository
public interface MandalRepository extends JpaRepository<Mandal, UUID> {

    /**
     * Returns all active mandals belonging to the given district, ordered by name.
     *
     * @param districtId the parent district UUID
     * @return list of active mandals for the district, sorted alphabetically
     */
    @Query("SELECT m FROM Mandal m WHERE m.district.id = :districtId AND m.isActive = true ORDER BY m.name ASC")
    List<Mandal> findActiveByDistrictId(@Param("districtId") UUID districtId);

    @Query("SELECT m FROM Mandal m WHERE m.district.id = :districtId AND LOWER(TRIM(m.name)) = LOWER(TRIM(:name))")
    java.util.Optional<Mandal> findByDistrictIdAndNameIgnoreCase(@Param("districtId") UUID districtId, @Param("name") String name);

    @Query("SELECT m FROM Mandal m WHERE m.district.id = :districtId AND LOWER(TRIM(m.code)) = LOWER(TRIM(:code))")
    java.util.Optional<Mandal> findByDistrictIdAndCodeIgnoreCase(@Param("districtId") UUID districtId, @Param("code") String code);

    @Query("SELECT m FROM Mandal m WHERE m.district.id = :districtId")
    List<Mandal> findAllByDistrictId(@Param("districtId") UUID districtId);

    /**
     * Finds a mandal by LGD code within a state (via district.state).
     * Used during incremental sync when we have the state UUID but need to
     * resolve the mandal's parent without knowing the district code explicitly.
     */
    @Query("SELECT m FROM Mandal m WHERE m.district.state.id = :stateId AND LOWER(TRIM(m.code)) = LOWER(TRIM(:code))")
    java.util.Optional<Mandal> findByDistrictStateIdAndCodeIgnoreCase(@Param("stateId") UUID stateId, @Param("code") String code);

    /**
     * Counts all mandals belonging to a state in a single query.
     */
    @Query("SELECT COUNT(m) FROM Mandal m WHERE m.district.state.id = :stateId")
    long countByStateId(@Param("stateId") UUID stateId);
}
