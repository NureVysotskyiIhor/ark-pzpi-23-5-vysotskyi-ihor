package com.polls.backend.controller;

import com.polls.backend.entity.DeviceFingerprint;
import com.polls.backend.repository.DeviceFingerprintRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/device-fingerprints")
@Tag(name = "Device Fingerprints", description = "Управління відбитками пристроїв")
public class DeviceFingerprintController {

    @Autowired
    private DeviceFingerprintRepository deviceFingerprintRepository;

    @GetMapping
    @Operation(summary = "Отримати всі відбитки")
    public ResponseEntity<List<DeviceFingerprint>> getAllFingerprints() {
        return ResponseEntity.ok(deviceFingerprintRepository.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Отримати відбиток за ID")
    public ResponseEntity<DeviceFingerprint> getFingerprintById(@PathVariable UUID id) {
        return deviceFingerprintRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Створити новий відбиток")
    public ResponseEntity<DeviceFingerprint> createFingerprint(@RequestBody DeviceFingerprint fingerprint) {
        DeviceFingerprint saved = deviceFingerprintRepository.save(fingerprint);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Видалити відбиток")
    public ResponseEntity<Void> deleteFingerprint(@PathVariable UUID id) {
        if (deviceFingerprintRepository.existsById(id)) {
            deviceFingerprintRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/blocked")
    @Operation(summary = "Отримати всі заблоковані відбитки")
    public ResponseEntity<List<DeviceFingerprint>> getBlockedFingerprints() {
        return ResponseEntity.ok(deviceFingerprintRepository.findByIsBlockedTrue());
    }
}
