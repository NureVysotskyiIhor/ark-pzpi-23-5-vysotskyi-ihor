package com.polls.backend.service;

import com.polls.backend.entity.*;
import com.polls.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class DeviceFingerprintService {

    @Autowired
    private DeviceFingerprintRepository deviceFingerprintRepository;

    @Autowired
    private AdminLogRepository adminLogRepository;

    @Autowired
    private VoteRepository voteRepository;

    private static final Logger logger = LoggerFactory.getLogger(DeviceFingerprintService.class);
    // ========================================================================
    // БІЗНЕС-ЛОГІКА: Генерація та управління відбитками пристроїв
    // ========================================================================

    /**
     * CRUD: Получить все отпечатки
     */
    public List<DeviceFingerprint> getAll() {
        return deviceFingerprintRepository.findAll();
    }

    /**
     * CRUD: Получить отпечаток по ID
     */
    public DeviceFingerprint getFingerprintById(UUID id) {
        return deviceFingerprintRepository.findById(id).orElse(null);
    }

    /**
     * CRUD: Удалить отпечаток
     */
    public boolean deleteFingerprint(UUID id) {
        if (deviceFingerprintRepository.existsById(id)) {
            deviceFingerprintRepository.deleteById(id);
            logger.info("Відбиток видалено: {}", id);
            return true;
        }
        logger.warn("Відбиток не знайдено для видалення: {}", id);
        return false;
    }

    /**
     * Генерація унікального відбитка пристрою
     * МАТЕМАТИЧНІ МЕТОДИ:
     * - SHA-256 хеширование: hash = SHA256(ip + userAgent + timestamp)
     */
    public String generateFingerprintHash(String ip, String userAgent) {
        try {
            // Комбінуємо дані пристрою
            String combined = ip + "|" + userAgent + "|" + System.nanoTime();

            // МАТЕМАТИКА: SHA-256 хеш
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(combined.getBytes(StandardCharsets.UTF_8));

            // Конвертуємо в hex-строку
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Fingerprint generation failed", e);
        }
    }

    /**
     * Отримання або створення відбитка пристрою
     */
    public DeviceFingerprint getOrCreateFingerprint(String ip, String userAgent) {
        String fingerprintHash = generateFingerprintHash(ip, userAgent);

        Optional<DeviceFingerprint> existing = deviceFingerprintRepository
                .findByFingerprintHash(fingerprintHash);

        if (existing.isPresent()) {
            // Оновлюємо час останнього спостереження
            DeviceFingerprint fp = existing.get();
            fp.setLastSeen(LocalDateTime.now());
            return deviceFingerprintRepository.save(fp);
        }

        // Створюємо новий відбиток
        DeviceFingerprint newFingerprint = new DeviceFingerprint();
        newFingerprint.setFingerprintHash(fingerprintHash);
        newFingerprint.setIp(ip);
        newFingerprint.setUserAgent(userAgent);
        newFingerprint.setCreatedAt(LocalDateTime.now());
        newFingerprint.setLastSeen(LocalDateTime.now());
        newFingerprint.setIsBlocked(false);

        return deviceFingerprintRepository.save(newFingerprint);
    }

    // ========================================================================
    // БІЗНЕС-ЛОГІКА: Управління блокуванням пристроїв
    // ========================================================================

    /**
     * Блокування пристрою адміністратором
     */
    public DeviceFingerprint blockFingerprint(UUID fingerprintId, UUID adminId, String reason) {
        Optional<DeviceFingerprint> fpOpt = deviceFingerprintRepository.findById(fingerprintId);
        if (fpOpt.isEmpty()) {
            return null;
        }

        DeviceFingerprint fp = fpOpt.get();
        fp.setIsBlocked(true);
        fp.setBlockReason(reason);
        fp.setBlockedAt(LocalDateTime.now());
        fp.setBlockedByAdmin(new Admin() {{ setId(adminId); }});

        logAdminAction(adminId, "BLOCK_DEVICE", "DeviceFingerprint", fingerprintId,
                "Blocked device: " + reason);

        return deviceFingerprintRepository.save(fp);
    }

    /**
     * Розблокування пристрою
     */
    public DeviceFingerprint unblockFingerprint(UUID fingerprintId, UUID adminId) {
        Optional<DeviceFingerprint> fpOpt = deviceFingerprintRepository.findById(fingerprintId);
        if (fpOpt.isEmpty()) {
            return null;
        }

        DeviceFingerprint fp = fpOpt.get();
        fp.setIsBlocked(false);
        fp.setBlockReason(null);
        fp.setBlockedAt(null);
        fp.setBlockedByAdmin(null);

        logAdminAction(adminId, "UNBLOCK_DEVICE", "DeviceFingerprint", fingerprintId,
                "Unblocked device");

        return deviceFingerprintRepository.save(fp);
    }

    /**
     * Отримання списку заблокованих пристроїв
     */
    public List<DeviceFingerprint> getBlockedFingerprints() {
        return deviceFingerprintRepository.findByIsBlockedTrue();
    }

    /**
     * Статистика по активності пристрою
     */
    public Map<String, Object> getDeviceStatistics(UUID fingerprintId) {
        Optional<DeviceFingerprint> fpOpt = deviceFingerprintRepository.findById(fingerprintId);
        if (fpOpt.isEmpty()) {
            return null;
        }

        DeviceFingerprint fp = fpOpt.get();
        List<Vote> deviceVotes = voteRepository.findByFingerprint(fp);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("fingerprintId", fingerprintId);
        stats.put("ip", fp.getIp());
        stats.put("createdAt", fp.getCreatedAt());
        stats.put("lastSeen", fp.getLastSeen());
        stats.put("isBlocked", fp.getIsBlocked());
        stats.put("totalVotes", deviceVotes.size());
        stats.put("blockReason", fp.getBlockReason());

        return stats;
    }

    // ========================================================================
    // ДОПОМІЖНІ МЕТОДИ
    // ========================================================================

    private void logAdminAction(UUID adminId, String action, String targetType,
                                UUID targetId, String description) {
        AdminLog log = new AdminLog();
        log.setAdmin(new Admin() {{ setId(adminId); }});
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDescription(description);
        log.setCreatedAt(LocalDateTime.now());
        adminLogRepository.save(log);
    }
}