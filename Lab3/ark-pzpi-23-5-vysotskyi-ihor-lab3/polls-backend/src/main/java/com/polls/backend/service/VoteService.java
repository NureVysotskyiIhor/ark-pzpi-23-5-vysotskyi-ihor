package com.polls.backend.service;

import com.polls.backend.entity.*;
import com.polls.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class VoteService {

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private PollRepository pollRepository;

    @Autowired
    private DeviceFingerprintRepository deviceFingerprintRepository;

    @Autowired
    private AdminLogRepository adminLogRepository;

    @Autowired
    private PollService pollService;

    @Autowired
    private WebSocketBroadcaster webSocketBroadcaster;

    /**
     * CRUD: Получить все голоса
     */
    public List<Vote> getAll() {
        return voteRepository.findAll();
    }

    /**
     * CRUD: Получить голос по ID
     */
    public Vote getVoteById(UUID id) {
        return voteRepository.findById(id).orElse(null);
    }

    /**
     * Перевірка, чи вже голосував цей пристрій за це голосування
     */
    public boolean hasAlreadyVoted(UUID pollId, UUID fingerprintId) {
        Poll poll = pollRepository.findById(pollId).orElse(null);
        DeviceFingerprint fingerprint = deviceFingerprintRepository.findById(fingerprintId).orElse(null);

        if (poll == null || fingerprint == null) {
            return false;
        }

        Optional<Vote> existingVote = voteRepository.findByPollAndFingerprint(poll, fingerprint);
        return existingVote.isPresent();
    }

    /**
     * Реєстрація голосу з валідацією
     * 1. Перевіряємо, чи не голосував вже
     * 2. Перевіряємо, чи не заблокований пристрій
     * 3. Записуємо голос
     * 4. ⭐ Трансльуємо оновлені результати через WebSocket
     */
    public Vote registerVote(UUID pollId, UUID optionId, UUID fingerprintId) {
        System.out.println("🔥 registerVote called for pollId: " + pollId);

        // Перевірка на повторне голосування
        if (hasAlreadyVoted(pollId, fingerprintId)) {
            System.out.println("❌ Already voted");
            return null;
        }

        // Перевірка блокування пристрою
        DeviceFingerprint fingerprint = deviceFingerprintRepository.findById(fingerprintId).orElse(null);
        if (fingerprint == null || fingerprint.getIsBlocked()) {
            System.out.println("❌ Device blocked");
            return null;
        }

        // Отримуємо варіант
        Poll poll = pollRepository.findById(pollId).orElse(null);
        if (poll == null) {
            System.out.println("❌ Poll not found");
            return null;
        }

        // Створюємо голос
        Vote vote = new Vote();
        vote.setPoll(poll);
        vote.setFingerprint(fingerprint);
        vote.setVotedAt(LocalDateTime.now());

        if (optionId != null) {
            vote.setOption(new PollOption() {{ setId(optionId); }});
        }

        Vote savedVote = voteRepository.save(vote);
        System.out.println("🔥 Vote saved: " + savedVote.getId());

        // ⭐ ТРИГГЕР: Трансляція оновлених результатів через WebSocket
        try {
            Map<String, Object> stats = pollService.getPollStatistics(pollId);
            System.out.println("🔥 Stats obtained: " + stats);

            webSocketBroadcaster.broadcastPollResults(pollId, stats);
            System.out.println("🔥 Broadcast called successfully!");
        } catch (Exception e) {
            System.err.println("❌ Помилка при трансляції результатів через WebSocket: " + e.getMessage());
            e.printStackTrace();
        }

        return savedVote;
    }

    /**
     * Видалення голосу
     */
    public boolean deleteVote(UUID voteId, UUID adminId) {
        if (voteRepository.existsById(voteId)) {
            Vote vote = voteRepository.findById(voteId).orElse(null);
            UUID pollId = vote != null ? vote.getPoll().getId() : null;

            voteRepository.deleteById(voteId);
            logAdminAction(adminId, "DELETE_VOTE", "Vote", voteId, "Deleted vote");

            // ⭐ ТРИГГЕР: Трансляція оновлених результатів після видалення
            if (pollId != null) {
                try {
                    Map<String, Object> stats = pollService.getPollStatistics(pollId);
                    webSocketBroadcaster.broadcastPollResults(pollId, stats);
                } catch (Exception e) {
                    System.err.println("❌ Помилка при трансляції результатів через WebSocket: " + e.getMessage());
                }
            }

            return true;
        }
        return false;
    }

    /**
     * Підрахунок кількості голосів від одного пристрою
     */
    public long countVotesFromFingerprint(UUID fingerprintId) {
        DeviceFingerprint fingerprint = deviceFingerprintRepository.findById(fingerprintId).orElse(null);
        if (fingerprint == null) {
            return 0;
        }
        return voteRepository.findByFingerprint(fingerprint).size();
    }

    /**
     * Обнаруження підозрілої активності
     */
    public double calculateAnomalyScore(UUID fingerprintId, UUID pollId) {
        long votesFromDevice = countVotesFromFingerprint(fingerprintId);

        List<DeviceFingerprint> allFingerprints = deviceFingerprintRepository.findAll();
        double averageVotesPerDevice = allFingerprints.stream()
                .mapToLong(fp -> voteRepository.findByFingerprint(fp).size())
                .average()
                .orElse(1.0);

        double anomalyScore = votesFromDevice / averageVotesPerDevice;

        return Math.round(anomalyScore * 100.0) / 100.0;
    }

    /**
     * Перевірка, чи аномальна активність пристрою
     */
    public boolean isAnomalousActivity(UUID fingerprintId) {
        double anomalyScore = calculateAnomalyScore(fingerprintId, null);
        return anomalyScore > 3.0;
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