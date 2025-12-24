package com.polls.backend.controller;

import com.polls.backend.dto.*;
import com.polls.backend.entity.Poll;
import com.polls.backend.service.ExportService;
import com.polls.backend.service.PollService;
import com.polls.backend.service.QrCodeService;
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
@RequestMapping("/api/polls")
@Tag(name = "Polls", description = "Управління голосуваннями")
public class PollController {

    @Autowired
    private PollService pollService;

    // ========================================================================
    // GET - Отримання голосувань
    // ========================================================================

    /**
     * Отримати всі голосування
     */
    @GetMapping
    @Operation(summary = "Отримати всі голосування")
    public ResponseEntity<List<Poll>> getAllPolls() {
        List<Poll> polls = pollService.getAll();
        return ResponseEntity.ok(polls);
    }

    /**
     * Отримати голосування за ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Отримати голосування за ID")
    public ResponseEntity<Poll> getPollById(@PathVariable UUID id) {
        Poll poll = pollService.getPollById(id);
        if (poll == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(poll);
    }

    /**
     * Отримати голосування за статусом
     */
    @GetMapping("/status/{status}")
    @Operation(summary = "Отримати голосування за статусом")
    public ResponseEntity<List<Poll>> getPollsByStatus(@PathVariable String status) {
        List<Poll> polls = pollService.getPollsByStatus(status);
        return ResponseEntity.ok(polls);
    }

    /**
     * Отримати гарячі голосування (з найбільшою активністю)
     */
    @GetMapping("/trending")
    @Operation(summary = "Отримати гарячі голосування")
    public ResponseEntity<List<Poll>> getTrendingPolls(
            @RequestParam(defaultValue = "10") int limit) {
        List<Poll> polls = pollService.getTrendingPolls(limit);
        return ResponseEntity.ok(polls);
    }

    // ========================================================================
    // POST - Створення голосування
    // ========================================================================

    /**
     * Створити нове голосування
     */
    @PostMapping
    @Operation(summary = "Створити нове голосування")
    public ResponseEntity<Poll> createPoll(
            @Valid @RequestBody CreatePollRequestDTO request) {

        try {
            Poll poll = pollService.createPoll(
                    request.getTitle(),
                    request.getQuestion(),
                    request.getType(),
                    request.getMultipleAnswers(),
                    request.getShowResults(),
                    UUID.fromString(request.getOrganizerFingerprintId())
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(poll);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ========================================================================
    // PUT - Оновлення голосування
    // ========================================================================

    /**
     * Оновити голосування
     */
    @PutMapping("/{id}")
    @Operation(summary = "Оновити голосування")
    public ResponseEntity<Poll> updatePoll(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePollRequestDTO request) {

        try {
            Poll poll = pollService.updatePoll(id, request);
            return ResponseEntity.ok(poll);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ========================================================================
    // DELETE - Видалення голосування
    // ========================================================================

    /**
     * Видалити голосування
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Видалити голосування")
    public ResponseEntity<Void> deletePoll(@PathVariable UUID pollId) {
        boolean deleted = pollService.deletePoll(pollId, null);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    // ========================================================================
    // СТАТИСТИКА - Отримання результатів і метрик
    // ========================================================================

    /**
     * Отримати статистику голосування
     * ✅ ВИПРАВЛЕНО: Правильна конвертація типів
     */
    @GetMapping("/{id}/statistics")
    @Operation(summary = "Отримати статистику голосування")
    public ResponseEntity<PollStatisticsResponseDTO> getPollStatistics(
            @PathVariable UUID id) {

        try {
            Map<String, Object> stats = pollService.getPollStatistics(id);
            if (stats == null) {
                return ResponseEntity.notFound().build();
            }

            PollStatisticsResponseDTO response = convertToStatisticsDTO(id, stats);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Отримати математичні метрики розподілу голосів
     * ✅ ВИПРАВЛЕНО: Правильна робота з типами Map<String, Double>
     */
    @GetMapping("/{id}/metrics")
    @Operation(summary = "Отримати метрики розподілу голосів")
    public ResponseEntity<DistributionMetricsResponseDTO> getDistributionMetrics(
            @PathVariable UUID id) {

        try {
            Map<String, Double> metrics = pollService.calculateDistributionMetrics(id);
            if (metrics == null || metrics.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            DistributionMetricsResponseDTO response = new DistributionMetricsResponseDTO();
            response.setMean(metrics.getOrDefault("mean", 0.0));
            response.setStdDeviation(metrics.getOrDefault("stdDeviation", 0.0));
            response.setCoefficientOfVariation(metrics.getOrDefault("coefficientOfVariation", 0.0));
            response.setMin(metrics.getOrDefault("min", 0.0));
            response.setMax(metrics.getOrDefault("max", 0.0));
            response.setPollId(id.toString());

            // totalVotes is Long, convert from Double if needed
            Double totalVotesDouble = metrics.get("totalVotes");
            if (totalVotesDouble != null) {
                response.setTotalVotes(totalVotesDouble.longValue());
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========================================================================
    // ДОПОМІЖНІ МЕТОДИ ДЛЯ КОНВЕРТАЦІЇ
    // ========================================================================

    @Autowired
    private ExportService exportService;

    @Autowired
    private QrCodeService qrCodeService;

    /**
     * Получить QR-код для голосования
     * GET /api/polls/{id}/qr
     */
    @GetMapping("/{id}/qr")
    @Operation(summary = "Получить QR-код")
    public ResponseEntity<QrCodeService.QrCodeInfoDTO> getQrCode(@PathVariable UUID id) {
        try {
            Poll poll = pollService.getPollById(id);
            if (poll == null) {
                return ResponseEntity.notFound().build();
            }

            QrCodeService.QrCodeInfoDTO qrInfo = qrCodeService.getQrCodeInfo(id);
            return ResponseEntity.ok(qrInfo);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Экспортировать результаты голосования в CSV
     * GET /api/polls/{id}/export/csv
     */
    @GetMapping("/{id}/export/csv")
    @Operation(summary = "Экспортировать результаты в CSV")
    public ResponseEntity<String> exportPollCsv(@PathVariable UUID id) {
        try {
            Poll poll = pollService.getPollById(id);
            if (poll == null) {
                return ResponseEntity.notFound().build();
            }

            String csv = exportService.exportPollToCsv(id);
            if (csv == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=poll_" + id + "_statistics.csv")
                    .header("Content-Type", "text/csv;charset=UTF-8")
                    .body(csv);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Экспортировать все опросы в CSV
     * GET /api/polls/export/csv
     */
    @GetMapping("/export/csv")
    @Operation(summary = "Экспортировать все опросы в CSV")
    public ResponseEntity<String> exportAllPollsCsv() {
        try {
            String csv = exportService.exportAllPollsToCsv();
            if (csv == null || csv.isEmpty()) {
                return ResponseEntity.noContent().build();
            }

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=all_polls.csv")
                    .header("Content-Type", "text/csv;charset=UTF-8")
                    .body(csv);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Экспортировать детали голосов в CSV
     * GET /api/polls/{id}/votes/export/csv
     */
    @GetMapping("/{id}/votes/export/csv")
    @Operation(summary = "Экспортировать голосы опроса в CSV")
    public ResponseEntity<String> exportVotesCsv(@PathVariable UUID id) {
        try {
            Poll poll = pollService.getPollById(id);
            if (poll == null) {
                return ResponseEntity.notFound().build();
            }

            String csv = exportService.exportVoteDetailsToCsv(id);
            if (csv == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=poll_" + id + "_votes.csv")
                    .header("Content-Type", "text/csv;charset=UTF-8")
                    .body(csv);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Экспортировать опрос в PDF (текстовый формат)
     * GET /api/polls/{id}/export/pdf
     */
    @GetMapping("/{id}/export/pdf")
    @Operation(summary = "Экспортировать в PDF")
    public ResponseEntity<byte[]> exportPollPdf(@PathVariable UUID id) {
        Poll poll = pollService.getPollById(id);
        if (poll == null) {
            return ResponseEntity.notFound().build();
        }

        byte[] pdfBytes = exportService.generatePdfBytes(id);
        if (pdfBytes == null || pdfBytes.length == 0) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=poll_" + id + "_report.pdf")
                .header("Content-Type", "application/pdf")
                .body(pdfBytes);
    }




    /**
     * ✅ ВИПРАВЛЕНО: Правильна конвертація Map у PollStatisticsResponseDTO
     */
    @SuppressWarnings("unchecked")
    private PollStatisticsResponseDTO convertToStatisticsDTO(UUID pollId, Map<String, Object> stats) {
        PollStatisticsResponseDTO dto = new PollStatisticsResponseDTO();

        // Конвертація pollId з правильною обробкою типу
        Object pollIdObj = stats.get("pollId");
        if (pollIdObj instanceof UUID) {
            dto.setPollId(((UUID) pollIdObj).toString());
        } else if (pollIdObj != null) {
            dto.setPollId(pollIdObj.toString());
        } else {
            dto.setPollId(pollId.toString());
        }

        // Інші поля
        dto.setPollTitle((String) stats.get("pollTitle"));
        dto.setStatus((String) stats.get("status"));

        Object createdAtObj = stats.get("createdAt");
        if (createdAtObj != null) {
            dto.setCreatedAt((java.time.LocalDateTime) createdAtObj);
        }

        Object totalVotesObj = stats.get("totalVotes");
        if (totalVotesObj instanceof Long) {
            dto.setTotalVotes((Long) totalVotesObj);
        } else if (totalVotesObj instanceof Number) {
            dto.setTotalVotes(((Number) totalVotesObj).longValue());
        }

        // Конвертація опцій з обробкою помилок
        Object optionsObj = stats.get("options");
        if (optionsObj instanceof List) {
            List<Map<String, Object>> optionsMap = (List<Map<String, Object>>) optionsObj;
            List<PollStatisticsResponseDTO.OptionStatistics> options = optionsMap.stream()
                    .map(this::convertToOptionStatistics)
                    .toList();
            dto.setOptions(options);

            // Знаходимо лідера (максимум голосів)
            PollStatisticsResponseDTO.OptionStatistics leader = options.stream()
                    .max((o1, o2) -> Long.compare(o1.getVotes(), o2.getVotes()))
                    .orElse(null);
            dto.setLeader(leader);
        }

        return dto;
    }

    /**
     * Конвертація опції з Map до OptionStatistics
     * ✅ ВИПРАВЛЕНО: Правильна робота з типами Integer замість int
     */
    private PollStatisticsResponseDTO.OptionStatistics convertToOptionStatistics(Map<String, Object> optionMap) {
        PollStatisticsResponseDTO.OptionStatistics stat = new PollStatisticsResponseDTO.OptionStatistics();

        // Обробляємо optionId як UUID або String
        Object optionIdObj = optionMap.get("optionId");
        if (optionIdObj instanceof UUID) {
            stat.setOptionId(((UUID) optionIdObj).toString());
        } else if (optionIdObj != null) {
            stat.setOptionId(optionIdObj.toString());
        }

        stat.setOptionText((String) optionMap.get("optionText"));

        // votes як Long
        Object votesObj = optionMap.get("votes");
        if (votesObj instanceof Long) {
            stat.setVotes((Long) votesObj);
        } else if (votesObj instanceof Number) {
            stat.setVotes(((Number) votesObj).longValue());
        }

        // percentage як Double
        Object percentageObj = optionMap.get("percentage");
        if (percentageObj instanceof Double) {
            stat.setPercentage((Double) percentageObj);
        } else if (percentageObj instanceof Number) {
            stat.setPercentage(((Number) percentageObj).doubleValue());
        }

        // order як Integer
        Object orderObj = optionMap.get("order");
        if (orderObj instanceof Integer) {
            stat.setOrder((Integer) orderObj);
        } else if (orderObj instanceof Number) {
            stat.setOrder(((Number) orderObj).intValue());
        }

        return stat;
    }
}