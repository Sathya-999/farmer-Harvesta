package com.agriplatform.geography.repository;

import com.agriplatform.geography.entity.State;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Read-only repository for {@link State} entities.
 *
 * <p>Provides access to the {@code public.states} Supabase table.
 * No write operations are exposed — geography data is reference-only.
 *
 * <p>Spring Data JPA will generate efficient SELECT queries.
 * No custom queries are needed for the required API endpoints.
 */
@Repository
public interface StateRepository extends JpaRepository<State, UUID> {

    /**
     * Returns all active states ordered by name.
     *
     * @return list of active states sorted alphabetically
     */
    List<State> findByIsActiveTrueOrderByNameAsc();

    /**
     * Finds a state by name, case-insensitively with trimmed whitespace.
     */
    @Query("SELECT s FROM State s WHERE LOWER(TRIM(s.name)) = LOWER(TRIM(:name))")
    Optional<State> findByNameIgnoreCase(@Param("name") String name);

    /**
     * Finds a state by code, case-insensitively with trimmed whitespace.
     */
    @Query("SELECT s FROM State s WHERE LOWER(TRIM(s.code)) = LOWER(TRIM(:code))")
    Optional<State> findByCodeIgnoreCase(@Param("code") String code);

    /**
     * Retrieves counts of districts, mandals, and villages for a given state code in a single SQL round-trip.
     */
    @Query(value = "SELECT " +
            "(SELECT count(*) FROM public.districts d WHERE d.state_id = s.id) AS district_count, " +
            "(SELECT count(*) FROM public.mandals m JOIN public.districts d ON m.district_id = d.id WHERE d.state_id = s.id) AS mandal_count, " +
            "(SELECT count(*) FROM public.villages v JOIN public.mandals m ON v.mandal_id = m.id JOIN public.districts d ON m.district_id = d.id WHERE d.state_id = s.id) AS village_count " +
            "FROM public.states s WHERE LOWER(TRIM(s.code)) = LOWER(TRIM(:code))", nativeQuery = true)
    List<Object[]> getCountsByStateCode(@Param("code") String code);
}
