package com.polls.backend.service;

import com.polls.backend.entity.*;
import com.polls.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class VoteService {

    private static final Logger logger = LoggerFactory.getLogger(VoteService.class);

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private PollRepository pollRepository;

    @Autowired
    private DeviceFingerprintRepository deviceFingerprintRepository;

    @Autowired
    private AdminLogRepository adminLogRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private PollOptionRepository pollOptionRepository;

    @Autowired
    private AuditService auditService;

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
        logger.info("registerVote called for pollId: {}", pollId);

        // Перевірка на повторне голосування
        if (hasAlreadyVoted(pollId, fingerprintId)) {
            logger.warn("Vote rejected: already voted for pollId: {}", pollId);
            return null;
        }

        // Перевірка блокування пристрою
        DeviceFingerprint fingerprint = deviceFingerprintRepository.findById(fingerprintId).orElse(null);
        if (fingerprint == null || fingerprint.getIsBlocked()) {
            logger.warn("Vote rejected: device blocked, fingerprintId: {}", fingerprintId);
            return null;
        }

        // Отримуємо варіант
        Poll poll = pollRepository.findById(pollId).orElse(null);
        if (poll == null) {
            logger.warn("Vote rejected: poll not found, pollId: {}", pollId);
            return null;
        }

        // Створюємо голос
        Vote vote = new Vote();
        vote.setPoll(poll);
        vote.setFingerprint(fingerprint);
        vote.setVotedAt(LocalDateTime.now());

        if (optionId != null) {
            vote.setOption(pollOptionRepository.getReferenceById(optionId));
        }

        Vote savedVote = voteRepository.save(vote);
        logger.info("Vote saved: {}", savedVote.getId());

        // Трансляція оновлених результатів через WebSocket
        try {
            Map<String, Object> stats = pollService.getPollStatistics(pollId);
            logger.debug("Poll stats obtained for broadcast: pollId={}", pollId);

            webSocketBroadcaster.broadcastPollResults(pollId, stats);
            logger.info("WebSocket broadcast completed for pollId: {}", pollId);
        } catch (Exception e) {
            logger.error("Failed to broadcast results via WebSocket for pollId: {}", pollId, e);
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
            auditService.log(adminId, "DELETE_VOTE", "Vote", voteId, "Deleted vote");

            // ⭐ ТРИГГЕР: Трансляція оновлених результатів після видалення
            if (pollId != null) {
                try {
                    Map<String, Object> stats = pollService.getPollStatistics(pollId);
                    webSocketBroadcaster.broadcastPollResults(pollId, stats);
                } catch (Exception e) {
                    logger.error("Failed to broadcast results via WebSocket for pollId: {}", pollId, e);
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

}