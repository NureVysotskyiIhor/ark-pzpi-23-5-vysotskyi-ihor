package com.polls.backend.service;

import com.polls.backend.entity.*;
import com.polls.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AdminService {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private AdminLogRepository adminLogRepository;

    private static final Logger logger = LoggerFactory.getLogger(DeviceFingerprintService.class);

    // ========================================================================
    // БІЗНЕС-ЛОГІКА: Управління адміністраторами
    // ========================================================================

    /**
     * CRUD: Получить всех администраторов
     */
    public List<Admin> getAll() {
        return adminRepository.findAll();
    }

    /**
     * CRUD: Получить администратора по ID
     */
    public Admin getAdminById(UUID id) {
        return adminRepository.findById(id).orElse(null);
    }

    /**
     * CRUD: Получить администратора по email
     */
    public Admin getAdminByEmail(String email) {
        return adminRepository.findByEmail(email).orElse(null);
    }

    /**
     * CRUD: Удалить администратора
     */
    public boolean deleteAdmin(UUID id) {
        if (adminRepository.existsById(id)) {
            adminRepository.deleteById(id);
            logger.info("Адміністратор видалено: {}", id);
            return true;
        }
        logger.warn("Адміністратор не знайдено для видалення: {}", id);
        return false;
    }

    /**
     * Реєстрація нового адміністратора
     */
    public Admin registerAdmin(String email, String passwordHash, String name) {
        if (adminRepository.existsByEmail(email)) {
            return null; // Помилка: email вже зареєстрований
        }

        Admin admin = new Admin();
        admin.setEmail(email);
        admin.setPasswordHash(passwordHash);
        admin.setName(name);
        admin.setIsActive(true);
        admin.setCreatedAt(LocalDateTime.now());

        return adminRepository.save(admin);
    }

    /**
     * Деактивація адміністратора
     */
    public Admin deactivateAdmin(UUID adminId, UUID requestingAdminId) {
        Optional<Admin> adminOpt = adminRepository.findById(adminId);
        if (adminOpt.isEmpty()) {
            return null;
        }

        Admin admin = adminOpt.get();
        admin.setIsActive(false);

        logAdminAction(requestingAdminId, "DEACTIVATE_ADMIN", "Admin", adminId,
                "Deactivated admin: " + admin.getEmail());

        return adminRepository.save(admin);
    }

    /**
     * Оновлення часу останнього входу адміністратора
     */
    public void updateLastLogin(UUID adminId) {
        Optional<Admin> adminOpt = adminRepository.findById(adminId);
        adminOpt.ifPresent(admin -> {
            admin.setLastLoginAt(LocalDateTime.now());
            adminRepository.save(admin);
        });
    }

    // ========================================================================
    // БІЗНЕС-ЛОГІКА: Управління логами
    // ========================================================================

    /**
     * Отримання всіх логів дій адміністраторів
     */
    public List<Map<String, Object>> getAllLogs() {
        return adminLogRepository.findAll().stream()
                .map(this::convertLogToMap)
                .collect(Collectors.toList());
    }

    /**
     * Отримання логів за типом дії
     */
    public List<Map<String, Object>> getLogsByAction(String action) {
        return adminLogRepository.findByAction(action).stream()
                .map(this::convertLogToMap)
                .collect(Collectors.toList());
    }

    /**
     * Отримання логів за типом цілі
     */
    public List<Map<String, Object>> getLogsByTargetType(String targetType) {
        return adminLogRepository.findByTargetType(targetType).stream()
                .map(this::convertLogToMap)
                .collect(Collectors.toList());
    }

    /**
     * Отримання логів за адміністратором
     */
    public List<Map<String, Object>> getLogsByAdmin(UUID adminId) {
        Optional<Admin> adminOpt = adminRepository.findById(adminId);
        if (adminOpt.isEmpty()) {
            return Collections.emptyList();
        }

        return adminLogRepository.findByAdmin(adminOpt.get()).stream()
                .map(this::convertLogToMap)
                .collect(Collectors.toList());
    }

    /**
     * Статистика дій адміністраторів за період
     * МАТЕМАТИЧНІ МЕТОДИ:
     * - Групування та підрахунок по типам дій
     * - Розрахунок відсотків для кожного типу
     */
    public Map<String, Object> getAdminActivityStatistics(LocalDateTime from, LocalDateTime to) {
        List<AdminLog> logs = adminLogRepository.findAll().stream()
                .filter(log -> log.getCreatedAt().isAfter(from) && log.getCreatedAt().isBefore(to))
                .collect(Collectors.toList());

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("period", from + " to " + to);
        stats.put("totalActions", logs.size());

        // МАТЕМАТИКА: Групування та підрахунок
        Map<String, Long> actionCounts = logs.stream()
                .collect(Collectors.groupingBy(AdminLog::getAction, Collectors.counting()));

        // МАТЕМАТИКА: Розрахунок відсотків
        Map<String, Double> actionPercentages = new LinkedHashMap<>();
        actionCounts.forEach((action, count) -> {
            double percentage = logs.size() > 0
                    ? (count * 100.0) / logs.size()
                    : 0.0;
            actionPercentages.put(action, Math.round(percentage * 100.0) / 100.0);
        });

        stats.put("actionCounts", actionCounts);
        stats.put("actionPercentages", actionPercentages);

        return stats;
    }

    // ========================================================================
    // ДОПОМІЖНІ МЕТОДИ
    // ========================================================================

    private Map<String, Object> convertLogToMap(AdminLog log) {
        Map<String, Object> logMap = new LinkedHashMap<>();
        logMap.put("id", log.getId());
        logMap.put("adminId", log.getAdmin() != null ? log.getAdmin().getId() : null);
        logMap.put("action", log.getAction());
        logMap.put("targetType", log.getTargetType());
        logMap.put("targetId", log.getTargetId());
        logMap.put("description", log.getDescription());
        logMap.put("createdAt", log.getCreatedAt());
        return logMap;
    }

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