package com.polls.backend.service;

import com.polls.backend.dto.UpdatePollRequestDTO;
import com.polls.backend.entity.*;
import com.polls.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PollService {

    @Autowired
    private PollRepository pollRepository;

    @Autowired
    private PollOptionRepository pollOptionRepository;

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private AdminLogRepository adminLogRepository;

    // ========================================================================
    // БІЗНЕС-ЛОГІКА: Управління голосуваннями
    // ========================================================================

    /**
     * CRUD: Получить все голосования
     */
    public List<Poll> getAll() {
        return pollRepository.findAll();
    }

    /**
     * CRUD: Получить голосование по ID
     */
    public Poll getPollById(UUID id) {
        return pollRepository.findById(id).orElse(null);
    }

    /**
     * CRUD: Получить голосования по статусу
     */
    public List<Poll> getPollsByStatus(String status) {
        return pollRepository.findByStatus(status);
    }

    /**
     * CRUD: Обновить голосование
     * РЕФАКТОРИНГ: Принимает UpdatePollRequestDTO вместо множества параметров
     */
    public Poll updatePoll(UUID id, UpdatePollRequestDTO request) {
        Optional<Poll> pollOpt = pollRepository.findById(id);
        if (pollOpt.isEmpty()) {
            throw new IllegalArgumentException("Poll не знайдено");
        }

        Poll poll = pollOpt.get();

        if (request.getTitle() != null && !request.getTitle().isEmpty()) {
            poll.setTitle(request.getTitle());
        }
        if (request.getQuestion() != null && !request.getQuestion().isEmpty()) {
            poll.setQuestion(request.getQuestion());
        }
        if (request.getStatus() != null) {
            poll.setStatus(request.getStatus());
        }
        if (request.getShowResults() != null) {
            poll.setShowResults(request.getShowResults());
        }

        return pollRepository.save(poll);
    }

    /**
     * CRUD: Создать голосование с параметрами
     */
    public Poll createPoll(String title, String question, String type,
                           Boolean multipleAnswers, Boolean showResults, UUID organizerFingerprintId) {

        // Валідація
        if (title == null || title.isEmpty()) {
            throw new IllegalArgumentException("Назва не може бути пустою");
        }
        if (question == null || question.isEmpty()) {
            throw new IllegalArgumentException("Питання не може бути пустим");
        }

        Poll poll = new Poll();
        poll.setTitle(title);
        poll.setQuestion(question);
        poll.setType(type);
        poll.setMultipleAnswers(multipleAnswers != null ? multipleAnswers : false);
        poll.setShowResults(showResults != null ? showResults : true);
        poll.setStatus("ACTIVE");
        poll.setCreatedAt(LocalDateTime.now());

        // Установить організатора
        DeviceFingerprint organizer = new DeviceFingerprint();
        organizer.setId(organizerFingerprintId);
        poll.setOrganizerFingerprint(organizer);

        return pollRepository.save(poll);
    }

    /**
     * Створення нового голосування
     * Генерує унікальний ID та ініціалізує базові параметри
     */
    public Poll createPoll(Poll poll) {
        poll.setStatus("ACTIVE");
        poll.setCreatedAt(LocalDateTime.now());
        return pollRepository.save(poll);
    }

    /**
     * Закриття голосування адміністратором
     * Встановлює статус CLOSED та час закриття
     */
    public Poll closePoll(UUID pollId, UUID adminId) {
        Optional<Poll> pollOpt = pollRepository.findById(pollId);
        if (pollOpt.isPresent()) {
            Poll poll = pollOpt.get();
            poll.setStatus("CLOSED");
            poll.setClosedAt(LocalDateTime.now());
            poll.setClosedByAdmin(new Admin() {{ setId(adminId); }});

            // Логування дії адміністратора
            logAdminAction(adminId, "CLOSE_POLL", "Poll", pollId,
                    "Closed poll: " + poll.getTitle());

            return pollRepository.save(poll);
        }
        return null;
    }

    /**
     * Архівування голосування
     */
    public Poll archivePoll(UUID pollId, UUID adminId) {
        Optional<Poll> pollOpt = pollRepository.findById(pollId);
        if (pollOpt.isPresent()) {
            Poll poll = pollOpt.get();
            poll.setStatus("ARCHIVED");

            logAdminAction(adminId, "ARCHIVE_POLL", "Poll", pollId,
                    "Archived poll: " + poll.getTitle());

            return pollRepository.save(poll);
        }
        return null;
    }

    /**
     * Видалення голосування адміністратором
     */
    public boolean deletePoll(UUID pollId, UUID adminId) {
        if (pollRepository.existsById(pollId)) {
            Poll poll = pollRepository.findById(pollId).get();

            logAdminAction(adminId, "DELETE_POLL", "Poll", pollId,
                    "Deleted poll: " + poll.getTitle());

            pollRepository.deleteById(pollId);
            return true;
        }
        return false;
    }

    // ========================================================================
    // БІЗНЕС-ЛОГІКА: Математичні методи підрахунку результатів
    // ========================================================================

    /**
     * Отримання статистики голосування
     * МАТЕМАТИЧНІ МЕТОДИ:
     * 1. Підрахунок голосів per option
     * 2. Розрахунок відсотків: percentage = (votes / total) * 100
     * 3. Визначення переможця (max votes)
     */
    public Map<String, Object> getPollStatistics(UUID pollId) {
        Optional<Poll> pollOpt = pollRepository.findById(pollId);
        if (pollOpt.isEmpty()) {
            return null;
        }

        Poll poll = pollOpt.get();
        List<Vote> votes = voteRepository.findByPoll(poll);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("pollId", pollId);
        stats.put("pollTitle", poll.getTitle());
        stats.put("status", poll.getStatus());
        stats.put("createdAt", poll.getCreatedAt());

        // Загальна кількість голосів
        long totalVotes = votes.size();
        stats.put("totalVotes", totalVotes);

        // Статистика по кожному варіанту
        List<Map<String, Object>> optionStats = new ArrayList<>();
        if (poll.getOptions() != null) {
            for (PollOption option : poll.getOptions()) {
                Map<String, Object> optionData = new LinkedHashMap<>();

                // МАТЕМАТИКА: Підрахунок голосів за варіантом
                long votesForOption = votes.stream()
                        .filter(v -> v.getOption() != null && v.getOption().getId().equals(option.getId()))
                        .count();

                // МАТЕМАТИКА: Розрахунок відсотка
                double percentage = totalVotes > 0
                        ? (votesForOption * 100.0) / totalVotes
                        : 0.0;

                optionData.put("optionId", option.getId());
                optionData.put("optionText", option.getText());
                optionData.put("votes", votesForOption);
                optionData.put("percentage", Math.round(percentage * 100.0) / 100.0);
                optionData.put("order", option.getOrderNum());

                optionStats.add(optionData);
            }
        }

        stats.put("options", optionStats);

        // МАТЕМАТИКА: Визначення лідера (максимум голосів)
        if (!optionStats.isEmpty()) {
            Map<String, Object> leader = optionStats.stream()
                    .max(Comparator.comparingLong(o -> ((Number) o.get("votes")).longValue()))
                    .orElse(null);
            stats.put("leader", leader);
        }

        return stats;
    }

    /**
     * Розрахунок математичних показників розподілу голосів
     * МАТЕМАТИЧНІ МЕТОДИ:
     * 1. Середнє значення (Mean)
     * 2. Стандартне відхилення (Standard Deviation)
     * 3. Коефіцієнт варіації (Variation Coefficient)
     */
    public Map<String, Double> calculateDistributionMetrics(UUID pollId) {
        Optional<Poll> pollOpt = pollRepository.findById(pollId);
        if (pollOpt.isEmpty()) {
            return new HashMap<>();
        }

        Poll poll = pollOpt.get();
        List<Vote> allVotes = voteRepository.findByPoll(poll);

        List<Double> ratingValues = new ArrayList<>();

        for (Vote vote : allVotes) {
            if (vote.getOption() != null) {
                double ratingValue = vote.getOption().getOrderNum() + 1;
                ratingValues.add(ratingValue);
            }
        }

        Map<String, Double> metrics = new LinkedHashMap<>();

        if (ratingValues.isEmpty()) {
            metrics.put("mean", 0.0);
            metrics.put("stdDeviation", 0.0);
            metrics.put("coefficientOfVariation", 0.0);
            metrics.put("min", 0.0);
            metrics.put("max", 0.0);
            metrics.put("totalVotes", 0.0);
            return metrics;
        }

        double sum = ratingValues.stream()
                .mapToDouble(Double::doubleValue)
                .sum();
        double mean = sum / ratingValues.size();
        metrics.put("mean", Math.round(mean * 100.0) / 100.0);

        double variance = ratingValues.stream()
                .mapToDouble(value -> Math.pow(value - mean, 2))
                .average()
                .orElse(0.0);
        double stdDeviation = Math.sqrt(variance);
        metrics.put("stdDeviation", Math.round(stdDeviation * 100.0) / 100.0);

        double coefficientOfVariation = (mean != 0)
                ? (stdDeviation / mean) * 100
                : 0.0;
        metrics.put("coefficientOfVariation", Math.round(coefficientOfVariation * 100.0) / 100.0);

        double min = ratingValues.stream()
                .mapToDouble(Double::doubleValue)
                .min()
                .orElse(0.0);
        double max = ratingValues.stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0.0);

        metrics.put("min", min);
        metrics.put("max", max);
        metrics.put("totalVotes", (double) ratingValues.size());

        return metrics;
    }

    /**
     * Отримання гарячих голосувань (з найбільшою активністю)
     */
    public List<Poll> getTrendingPolls(int limit) {
        List<Poll> allPolls = pollRepository.findByStatus("ACTIVE");

        return allPolls.stream()
                .sorted((p1, p2) -> {
                    long votes1 = voteRepository.countByPoll(p1);
                    long votes2 = voteRepository.countByPoll(p2);
                    return Long.compare(votes2, votes1); // Спадаючий порядок
                })
                .limit(limit)
                .collect(Collectors.toList());
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