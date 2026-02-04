package com.secureshop.idempotency.repo;

import com.secureshop.idempotency.domain.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {
    @Modifying
    @Transactional
    @Query(value = """
        INSERT IGNORE INTO idempotency_keys (user_id, endpoint, idem_key, request_hash, status, created_at)
        VALUES (:userId, :endpoint, :idemKey, :requestHash, 'IN_PROGRESS', CURRENT_TIMESTAMP)
        """, nativeQuery = true)
    int tryInsertIgnore(
            @Param("userId") Long userId,
            @Param("endpoint") String endpoint,
            @Param("idemKey") String idemKey,
            @Param("requestHash") String requestHash
    );
    Optional<IdempotencyKey> findByUserIdAndEndpointAndIdemKey(Long userId, String endpoint, String idemKey);
}