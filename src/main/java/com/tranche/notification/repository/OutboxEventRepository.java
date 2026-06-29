package com.tranche.notification.repository;

import com.tranche.notification.domain.OutboxEvent;
import com.tranche.notification.domain.OutboxEventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findTop50ByStatusOrderByCreatedAtAsc(OutboxEventStatus status);

    Page<OutboxEvent> findByStatus(OutboxEventStatus status, Pageable pageable);

    Page<OutboxEvent> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
