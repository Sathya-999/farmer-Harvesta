package com.agriplatform.farmer.repository;

import com.agriplatform.farmer.entity.FarmerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for {@link FarmerProfile} entities.
 */
@Repository
public interface FarmerProfileRepository extends JpaRepository<FarmerProfile, UUID> {

    /**
     * Loads a farmer profile by ID with full geography hierarchy eagerly fetched.
     * Uses LEFT JOINs so profiles without an assigned village are still loaded.
     *
     * @param id farmer UUID (matches Supabase auth.users ID)
     * @return optional containing the profile with resolved geography, or empty
     */
    @Query("SELECT fp FROM FarmerProfile fp " +
           "LEFT JOIN FETCH fp.homeVillage v " +
           "LEFT JOIN FETCH v.mandal m " +
           "LEFT JOIN FETCH m.district d " +
           "LEFT JOIN FETCH d.state s " +
           "WHERE fp.id = :id")
    Optional<FarmerProfile> findByIdWithGeography(@Param("id") UUID id);
}
