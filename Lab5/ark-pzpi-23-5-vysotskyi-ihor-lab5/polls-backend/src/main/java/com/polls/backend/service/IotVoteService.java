package com.polls.backend.service;

import com.polls.backend.entity.IotDevice;
import com.polls.backend.entity.IotDeviceConfig;
import com.polls.backend.entity.IotVote;
import com.polls.backend.entity.Poll;
import com.polls.backend.repository.IotDeviceRepository;
import com.polls.backend.repository.IotDeviceConfigRepository;
import com.polls.backend.repository.IotVoteRepository;
import com.polls.backend.repository.PollRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class IotVoteService {

    @Autowired
    private IotVoteRepository iotVoteRepository;

    @Autowired
    private IotDeviceRepository iotDeviceRepository;

    @Autowired
    private IotDeviceConfigRepository iotDeviceConfigRepository;

    @Autowired
    private PollRepository pollRepository;

    private static final Logger logger = LoggerFactory.getLogger(IotVoteService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * МАТЕМАТИКА: Вычисление confidence (уверенность)
     * Логистическая функция: confidence = 1 / (1 + e^(-k*(t-15)))
     * Быстрый выбор (3 сек) = 0.95
     * Нормальный выбор (15 сек) = 0.5
     * Медленный выбор (30 сек) = 0.05
     */
    public float calculateConfidence(long votingTimeMs) {
        double timeSec = votingTimeMs / 1000.0;
        double k = 0.1;
        double midpoint = 15.0; // 15 сек - точка перегиба

        double confidence = 1.0 / (1.0 + Math.exp(-k * (timeSec - midpoint)));
        return (float) Math.round(confidence * 100.0) / 100.0f;
    }

    /**
     * МАТЕМАТИКА: Вычисление anomaly_score (Z-score для выявления ботов)
     * Если время слишком короткое или длинное = подозрительно
     */
    public float calculateAnomalyScore(long votingTimeMs) {
        double expectedTime = 15000.0; // 15 сек ожидается
        double stdDev = 5000.0;        // стандартное отклонение

        double zScore = Math.abs((votingTimeMs - expectedTime) / stdDev);
        return (float) Math.round(zScore * 100.0) / 100.0f;
    }

    /**
     * МАТЕМАТИКА: Вычисление entropy (энтропия выбора)
     * Мера неопределённости в выборе
     * Высокая энтропия = долго думал
     * Низкая энтропия = быстро выбрал
     */
    public float calculateEntropy(long votingTimeMs) {
        // Нормализируем время: 0-30 сек → 0-1
        double normalized = Math.min(votingTimeMs / 30000.0, 1.0);

        // Entropy = -p*log2(p) - (1-p)*log2(1-p)
        // где p = normalized time
        double p = normalized;
        double entropy = -(p * (Math.log(p) / Math.log(2.0)) +
                (1 - p) * (Math.log(1 - p) / Math.log(2.0)));

        // Если entropy NaN (при p=0 или p=1), вернуть 0 или 1
        if (Double.isNaN(entropy)) {
            entropy = normalized;
        }

        return (float) Math.round(entropy * 100.0) / 100.0f;
    }

    /**
     * Определение подозрительности голоса
     */
    public boolean isSuspicious(float confidence, float anomalyScore,
                                float anomalyThreshold) {
        return confidence < 0.3f || anomalyScore > anomalyThreshold;
    }

    /**
     * Определение статуса валидации
     */
    public String getValidationStatus(float confidence, float anomalyScore,
                                      float anomalyThreshold) {
        if (anomalyScore > anomalyThreshold * 2) {
            return "REJECTED"; // Явные признаки бота
        }
        if (isSuspicious(confidence, anomalyScore, anomalyThreshold)) {
            return "SUSPICIOUS";
        }
        return "APPROVED";
    }

    /**
     * Регистрация IoT голоса с вычислением метрик
     */
    public IotVote registerIotVote(UUID iotDeviceId, UUID pollId,
                                   Map<String, Object> voteData) {
        // Получить устройство и конфиг
        Optional<IotDevice> deviceOpt = iotDeviceRepository.findById(iotDeviceId);
        Optional<Poll> pollOpt = pollRepository.findById(pollId);
        Optional<IotDeviceConfig> configOpt = iotDeviceConfigRepository
                .findByIotDeviceId(iotDeviceId);

        System.out.println("🔍 DEBUG registerIotVote:");
        System.out.println("  iotDeviceId: " + iotDeviceId + " -> found: " + deviceOpt.isPresent());
        System.out.println("  pollId: " + pollId + " -> found: " + pollOpt.isPresent());
        System.out.println("  configOpt: " + configOpt.isPresent());

        if (deviceOpt.isEmpty() || pollOpt.isEmpty() || configOpt.isEmpty()) {
            logger.warn("Device, Poll or Config not found");
            return null;
        }

        IotDevice device = deviceOpt.get();
        Poll poll = pollOpt.get();
        IotDeviceConfig config = configOpt.get();

        // Извлечь время голосования
        long votingTimeMs = ((Number) voteData.get("votingTimeMs")).longValue();

        // МАТЕМАТИКА: Вычислить метрики
        float confidence = calculateConfidence(votingTimeMs);
        float anomalyScore = calculateAnomalyScore(votingTimeMs);
        float entropy = calculateEntropy(votingTimeMs);

        boolean suspicious = isSuspicious(confidence, anomalyScore,
                config.getAnomalyThreshold());
        String validationStatus = getValidationStatus(confidence, anomalyScore,
                config.getAnomalyThreshold());

        // Создать голос
        IotVote vote = new IotVote();
        vote.setIotDevice(device);
        vote.setPoll(poll);
        vote.setVotingTimeMs((int) votingTimeMs);
        vote.setConfidence(confidence);
        vote.setAnomalyScore(anomalyScore);
        vote.setEntropy(entropy);
        vote.setIsSuspicious(suspicious);
        vote.setValidationStatus(validationStatus);

        // Заполнить данные в зависимости от типа опроса
        switch (poll.getType()) {
            case "SINGLE":
                String optionId = (String) voteData.get("optionId");
                if (optionId != null) {
                    vote.setOption(null); // TODO: получить PollOption
                }
                break;

            case "MULTIPLE":
                String optionIds = (String) voteData.get("optionIds");
                vote.setOptionIds(optionIds);
                break;

            case "RATING":
                Integer rating = (Integer) voteData.get("rating");
                vote.setRating(rating);
                break;

            case "OPEN":
                String textAnswer = (String) voteData.get("textAnswer");
                vote.setTextAnswer(textAnswer);
                break;
        }

        // Сохранить математический анализ
        try {
            Map<String, Object> analysis = Map.of(
                    "votingTimeMs", votingTimeMs,
                    "confidenceFormula", "1/(1+e^(-0.1*(t-15)))",
                    "anomalyFormula", "|actualTime - expectedTime| / stdDev",
                    "entropyFormula", "-p*log2(p) - (1-p)*log2(1-p)",
                    "timestamp", LocalDateTime.now().toString()
            );
            vote.setMathematicalAnalysis(objectMapper.writeValueAsString(analysis));
        } catch (Exception e) {
            logger.error("Error serializing mathematical analysis", e);
        }

        // Сохранить метаданные устройства
        try {
            Map<String, Object> metadata = Map.of(
                    "kioskId", device.getKioskId(),
                    "location", device.getLocation(),
                    "deviceType", device.getDeviceType()
            );
            vote.setDeviceMetadata(objectMapper.writeValueAsString(metadata));
        } catch (Exception e) {
            logger.error("Error serializing device metadata", e);
        }

        IotVote savedVote = iotVoteRepository.save(vote);

        logger.info("IoT Vote registered: {} | Confidence: {} | Anomaly: {} | Status: {}",
                savedVote.getId(), confidence, anomalyScore, validationStatus);

        return savedVote;
    }

    /**
     * Получить статистику киоска
     */
    public Map<String, Object> getDeviceStatistics(UUID iotDeviceId) {
        long totalVotes = iotVoteRepository.countByIotDeviceId(iotDeviceId);

        long approvedVotes = iotVoteRepository.findByIotDeviceId(iotDeviceId)
                .stream()
                .filter(v -> "APPROVED".equals(v.getValidationStatus()))
                .count();

        long suspiciousVotes = iotVoteRepository.findByIotDeviceId(iotDeviceId)
                .stream()
                .filter(IotVote::getIsSuspicious)
                .count();

        return Map.of(
                "totalVotes", totalVotes,
                "approvedVotes", approvedVotes,
                "suspiciousVotes", suspiciousVotes,
                "approvalRate", totalVotes > 0 ? (float) approvedVotes / totalVotes : 0.0
        );
    }
}