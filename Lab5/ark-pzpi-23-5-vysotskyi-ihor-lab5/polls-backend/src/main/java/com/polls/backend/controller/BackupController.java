package com.polls.backend.controller;

import com.polls.backend.service.BackupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/backups")
@Tag(name = "Backups", description = "Управління резервними копіями БД")
    public class BackupController {

    @Autowired
    private BackupService backupService;

    /**
     * POST /api/admin/backups
     * Створити резервну копію БД
     */
    @PostMapping
    @Operation(summary = "Створити резервну копію")
    public ResponseEntity<Map<String, String>> createBackup() {
        try {
            String filepath = backupService.createBackup();
            return ResponseEntity.status(201)
                    .body(Map.of(
                            "status", "Резервна копія створена",
                            "filepath", filepath,
                            "timestamp", java.time.LocalDateTime.now().toString()
                    ));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of(
                            "error", "Помилка при створенні резервної копії",
                            "details", e.getMessage()
                    ));
        }
    }

    /**
     * GET /api/admin/backups
     * Отримати список всіх резервних копій
     */
    @GetMapping
    @Operation(summary = "Отримати список резервних копій")
    public ResponseEntity<List<Map<String, Object>>> listBackups() {
        List<Map<String, Object>> backups = backupService.listBackups();
        return ResponseEntity.ok(backups);
    }

    /**
     * POST /api/admin/backups/restore
     * Відновити з резервної копії
     */
    @PostMapping("/restore")
    @Operation(summary = "Відновити з резервної копії")
    public ResponseEntity<Map<String, String>> restoreBackup(
            @RequestParam String filepath) {

        boolean success = backupService.restoreBackup(filepath);

        if (success) {
            return ResponseEntity.ok(Map.of(
                    "status", "Резервна копія відновлена успішно",
                    "filepath", filepath
            ));
        } else {
            return ResponseEntity.status(500)
                    .body(Map.of(
                            "error", "Помилка при відновленні резервної копії",
                            "filepath", filepath
                    ));
        }
    }

    /**
     * DELETE /api/admin/backups/old
     * Видалити старі резервні копії (старші за N днів)
     */
    @DeleteMapping("/old")
    @Operation(summary = "Видалити старі резервні копії")
    public ResponseEntity<Map<String, Object>> deleteOldBackups(
            @RequestParam(defaultValue = "7") int daysOld) {

        int deleted = backupService.deleteOldBackups(daysOld);

        return ResponseEntity.ok(Map.of(
                "status", "Старі резервні копії видалені",
                "daysOld", daysOld,
                "deletedCount", deleted
        ));
    }
}