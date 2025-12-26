package com.polls.backend.controller;

import com.polls.backend.dto.BlockDeviceRequestDTO;
import com.polls.backend.dto.UnBlockDeviceRequestDTO;
import com.polls.backend.entity.DeviceFingerprint;
import com.polls.backend.service.DeviceFingerprintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/device-fingerprints")
@Tag(name = "Device Fingerprints", description = "Управління відбитками пристроїв")
public class DeviceFingerprintController {

    @Autowired
    private DeviceFingerprintService deviceFingerprintService;

    /**
     * Отримати всі відбитки
     */
    @GetMapping
    @Operation(summary = "Отримати всі відбитки")
    public ResponseEntity<List<DeviceFingerprint>> getAllFingerprints() {
        List<DeviceFingerprint> fingerprints = deviceFingerprintService.getAll();
        return ResponseEntity.ok(fingerprints);
    }

    /**
     * Отримати відбиток за ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Отримати відбиток за ID")
    public ResponseEntity<DeviceFingerprint> getFingerprintById(@PathVariable UUID id) {
        DeviceFingerprint fingerprint = deviceFingerprintService.getFingerprintById(id);
        if (fingerprint == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(fingerprint);
    }

    /**
     * Отримати заблоковані відбитки
     */
    @GetMapping("/blocked")
    @Operation(summary = "Отримати заблоковані відбитки")
    public ResponseEntity<List<DeviceFingerprint>> getBlockedFingerprints() {
        List<DeviceFingerprint> blocked = deviceFingerprintService.getBlockedFingerprints();
        return ResponseEntity.ok(blocked);
    }

    /**
     * Створити новий відбиток
     * МАТЕМАТИКА: SHA-256 хеш від IP + UserAgent
     */
    @PostMapping
    @Operation(summary = "Створити новий відбиток")
    public ResponseEntity<DeviceFingerprint> createFingerprint(
            @RequestParam String ip,
            @RequestParam String userAgent) {

        try {
            DeviceFingerprint fingerprint = deviceFingerprintService
                    .getOrCreateFingerprint(ip, userAgent);
            return ResponseEntity.status(HttpStatus.CREATED).body(fingerprint);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Заблокувати пристрій адміністратором
     * РЕФАКТОРИНГ: @Valid валідує BlockDeviceRequestDTO
     */
    @PutMapping("/{adminId}/block")
    @Operation(summary = "Заблокувати пристрій адміністратором")
    public ResponseEntity<DeviceFingerprint> blockFingerprint(
            @PathVariable UUID adminId,
            @Valid @RequestBody BlockDeviceRequestDTO request) {

        try {
            // В request передаём fingerprintId и blockReason
            UUID fingerprintId = UUID.fromString(request.getFingerprintId());
            DeviceFingerprint blocked = deviceFingerprintService.blockFingerprint(
                    fingerprintId,
                    adminId,
                    request.getBlockReason()
            );

            if (blocked == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(blocked);
        } catch (IllegalArgumentException e) {
            // Если UUID невалидный
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * Розблокувати пристрій
     */
    @PutMapping("/{adminId}/unblock")
    public ResponseEntity<DeviceFingerprint> unblockFingerprint(
            @PathVariable UUID adminId,
            @Valid @RequestBody UnBlockDeviceRequestDTO request) {

        try {
            UUID fingerprintId = UUID.fromString(request.getFingerprintId());
            DeviceFingerprint unblocked = deviceFingerprintService.unblockFingerprint(
                    fingerprintId,
                    adminId
            );

            if (unblocked == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(unblocked);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Отримати статистику пристрою
     */
    @GetMapping("/{id}/statistics")
    @Operation(summary = "Отримати статистику пристрою")
    public ResponseEntity<Map<String, Object>> getDeviceStatistics(@PathVariable UUID id) {
        Map<String, Object> stats = deviceFingerprintService.getDeviceStatistics(id);
        if (stats == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(stats);
    }

    /**
     * Видалити відбиток
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Видалити відбиток")
    public ResponseEntity<Void> deleteFingerprint(@PathVariable UUID id) {
        boolean deleted = deviceFingerprintService.deleteFingerprint(id);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}