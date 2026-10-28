package com.agriplatform.plot.repository;

import com.agriplatform.plot.entity.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link IdempotencyKey} records.
 *
 * <p>Scoped queries always include {@code farmer_id} to ensure that
 * idempotency keys are isolated between farmers (different farmers
 * using the same key string are independent operations).
 */
@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, UUID> {

    /**
     * Looks up an existing idempotency record for the given farmer and key,
     * honoring the {@code expires_at} TTL.
     *
     * @param farmerId       authenticated farmer UUID
     * @param idempotencyKey the client-supplied idempotency key string
     * @return optional idempotency record if it exists and has not expired
     */
    @Query(nativeQuery = true,
           value = "SELECT * FROM public.idempotency_keys " +
                   "WHERE farmer_id = :farmerId " +
                   "  AND idempotency_key = :idempotencyKey " +
                   "  AND expires_at > now() " +
                   "LIMIT 1")
    Optional<IdempotencyKey> findValidByFarmerAndKey(
            @Param("farmerId") UUID farmerId,
            @Param("idempotencyKey") String idempotencyKey);
}
