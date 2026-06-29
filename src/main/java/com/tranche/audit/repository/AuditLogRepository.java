package com.tranche.audit.repository;

import com.tranche.audit.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtAsc(
            String entityType,
            Long entityId,
            Pageable pageable
    );

    @Query("""
            SELECT a FROM AuditLog a
            LEFT JOIN FETCH a.actor
            WHERE a.entityType = :entityType AND a.entityId = :entityId
            ORDER BY a.createdAt ASC
            """)
    List<AuditLog> findTimelineByEntity(
            @Param("entityType") String entityType,
            @Param("entityId") Long entityId
    );

    @Query("""
            SELECT a FROM AuditLog a
            LEFT JOIN FETCH a.actor
            WHERE (:entityType IS NULL OR a.entityType = :entityType)
              AND (:entityId IS NULL OR a.entityId = :entityId)
            """)
    Page<AuditLog> findFiltered(
            @Param("entityType") String entityType,
            @Param("entityId") Long entityId,
            Pageable pageable
    );
}
