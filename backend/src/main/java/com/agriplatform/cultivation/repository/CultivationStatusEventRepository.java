package com.agriplatform.cultivation.repository;

import com.agriplatform.cultivation.entity.CultivationStatusEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CultivationStatusEventRepository extends JpaRepository<CultivationStatusEvent, UUID> {

    /**
     * Non-paginated list for existing cultivation service usage.
     */
    List<CultivationStatusEvent> findAllByIntentionIdOrderByChangedAtAsc(UUID intentionId);

    /**
     * Paginated list for Task 09 History API.
     */
    Page<CultivationStatusEvent> findAllByIntentionIdOrderByChangedAtAsc(UUID intentionId, Pageable pageable);
}
