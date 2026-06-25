package com.skyline.org.auth.audit;

import com.skyline.org.auth.entity.AuthAuditEvent;
import com.skyline.org.auth.repository.AuthAuditEventRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthAuditPersistenceService {

    private final AuthAuditEventRepository auditEventRepository;

    public AuthAuditPersistenceService(AuthAuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Async("auditTaskExecutor")
    @Transactional
    public void persist(AuthEventType event, String subject, String clientIp, String detail) {
        auditEventRepository.save(new AuthAuditEvent(event, subject, clientIp, detail));
    }
}
