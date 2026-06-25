package com.skyline.org.auth.repository;

import com.skyline.org.auth.audit.AuthEventType;
import com.skyline.org.auth.entity.AuthAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface AuthAuditEventRepository extends JpaRepository<AuthAuditEvent, Long> {

    List<AuthAuditEvent> findByEventTypeAndSubjectOrderByOccurredAtDesc(AuthEventType eventType, String subject);

    @Modifying
    @Query("DELETE FROM AuthAuditEvent e WHERE e.occurredAt < :cutoff")
    int deleteByOccurredAtBefore(@Param("cutoff") Instant cutoff);
}
