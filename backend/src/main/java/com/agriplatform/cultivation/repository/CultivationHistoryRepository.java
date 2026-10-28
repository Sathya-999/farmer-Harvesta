package com.agriplatform.cultivation.repository;

import com.agriplatform.cultivation.entity.CultivationIntentionHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CultivationHistoryRepository extends JpaRepository<CultivationIntentionHistory, UUID> {

    /**
     * Returns all history snapshots for an intention, ordered by version descending.
     * Used by the existing cultivation service (non-paginated list).
     */
    List<CultivationIntentionHistory> findAllByIntentionIdOrderByVersionDesc(UUID intentionId);

    /**
     * Returns paginated history snapshots for an intention, ordered by version descending.
     * Used by the Task 09 History API.
     */
    Page<CultivationIntentionHistory> findAllByIntentionIdOrderByVersionDesc(UUID intentionId, Pageable pageable);

    /**
     * Retrieves a single history snapshot by its unique ID, verifying that the
     * parent cultivation intention belongs to the given farmer (IDOR protection).
     */
    @Query("SELECT h FROM CultivationIntentionHistory h " +
           "WHERE h.historyId = :historyId " +
           "  AND h.intentionId = :intentionId " +
           "  AND h.farmerId = :farmerId")
    Optional<CultivationIntentionHistory> findByHistoryIdAndIntentionIdAndFarmerId(
            @Param("historyId") UUID historyId,
            @Param("intentionId") UUID intentionId,
            @Param("farmerId") UUID farmerId);
}
