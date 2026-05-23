package com.polls.backend.service;

import com.polls.backend.entity.AdminLog;
import com.polls.backend.repository.AdminLogRepository;
import com.polls.backend.repository.AdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuditService {

    @Autowired
    private AdminLogRepository adminLogRepository;

    @Autowired
    private AdminRepository adminRepository;

    public void log(UUID adminId, String action, String targetType,
                    UUID targetId, String description) {
        AdminLog log = new AdminLog();
        log.setAdmin(adminRepository.getReferenceById(adminId));
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDescription(description);
        log.setCreatedAt(LocalDateTime.now());
        adminLogRepository.save(log);
    }
}
