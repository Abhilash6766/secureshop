package com.secureshop.audit.repo;

import com.secureshop.audit.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    @Query("""
            select a from AuditLog a
            where (:eventType is null or a.evettype = :eventType)
              and (:actorUserId is null or a.actorUserId = :actorUserId)
              and (:entityType is null or a.entityType = :entityType)
              and (:entityId is null or a.entityId = :entityId)
              and (:status is null or a.status = :status)
              and (:from is null or a.createdAt >= :from)
              and (:to is null or a.createdAt <= :to)
            """)
    Page<AuditLog> search(
            @Param("eventType") String eventType,
            @Param("actorUserId") Long actorUserId,
            @Param("entityType") String entityType,
            @Param("entityId") String entityId,
            @Param("status") String status,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable
    );
}
