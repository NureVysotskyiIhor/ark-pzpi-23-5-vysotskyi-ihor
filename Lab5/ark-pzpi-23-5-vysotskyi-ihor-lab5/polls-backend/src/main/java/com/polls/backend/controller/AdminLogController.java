package com.polls.backend.controller;

import com.polls.backend.entity.AdminLog;
import com.polls.backend.repository.AdminLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin-logs")
@Tag(name = "Admin Logs", description = "Логи дій адміністраторів")
public class AdminLogController {

    @Autowired
    private AdminLogRepository adminLogRepository;

    @GetMapping
    @Operation(summary = "Отримати всі логи")
    public ResponseEntity<List<AdminLog>> getAllLogs() {
        return ResponseEntity.ok(adminLogRepository.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Отримати лог за ID")
    public ResponseEntity<AdminLog> getLogById(@PathVariable UUID id) {
        return adminLogRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/action/{action}")
    @Operation(summary = "Отримати логи за дією")
    public ResponseEntity<List<AdminLog>> getLogsByAction(@PathVariable String action) {
        return ResponseEntity.ok(adminLogRepository.findByAction(action));
    }

    @GetMapping("/target-type/{targetType}")
    @Operation(summary = "Отримати логи за типом цілі")
    public ResponseEntity<List<AdminLog>> getLogsByTargetType(@PathVariable String targetType) {
        return ResponseEntity.ok(adminLogRepository.findByTargetType(targetType));
    }
}
