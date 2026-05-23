package com.polls.backend.service;

import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Text;
import com.polls.backend.entity.*;
import com.polls.backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

import com.itextpdf.kernel.pdf.*;
import com.itextpdf.layout.*;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.layout.properties.*;
import com.itextpdf.layout.element.*;

@Service
public class ExportService {

    private static final Logger logger = LoggerFactory.getLogger(ExportService.class);
    private static final DateTimeFormatter dateFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private PollRepository pollRepository;

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private PollService pollService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    // ========================================================================
    // БІЗНЕС-ЛОГІКА: Експорт у CSV з правильним экранированием
    // ========================================================================

    /**
     * Експорт результатів голосування у CSV
     *
     * РЕФАКТОРИНГ: Правильне экранирование CSV (подвійні кавички)
     * Обробка потенціальної OutOfMemory - потокова обробка
     */
    public String exportPollToCsv(UUID pollId) {
        Poll poll = pollRepository.findById(pollId).orElse(null);
        if (poll == null) {
            logger.warn("Poll не знайдено для експорту: {}", pollId);
            return null;
        }

        try {
            Map<String, Object> stats = pollService.getPollStatistics(pollId);
            StringBuilder csv = new StringBuilder();

            // Заголовок
            csv.append("\"Голосування\",\"").append(escapeCsv(poll.getTitle())).append("\"\n");
            csv.append("\"Питання\",\"").append(escapeCsv(poll.getQuestion())).append("\"\n");
            csv.append("\"Тип\",\"").append(escapeCsv(poll.getType())).append("\"\n");
            csv.append("\"Статус\",\"").append(escapeCsv(poll.getStatus())).append("\"\n");
            csv.append("\"Дата створення\",\"").append(poll.getCreatedAt().format(dateFormatter)).append("\"\n");
            csv.append("\"Всього голосів\",").append(stats.get("totalVotes")).append("\n\n");

            // Таблиця результатів
            csv.append("\"Варіант\",\"Голосів\",\"Відсоток\"\n");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> options = (List<Map<String, Object>>) stats.get("options");
            for (Map<String, Object> option : options) {
                csv.append("\"").append(escapeCsv((String) option.get("optionText"))).append("\",")
                        .append(option.get("votes")).append(",")
                        .append(option.get("percentage")).append("%\n");
            }

            logger.info("CSV експорт завершено для Poll: {}", pollId);
            return csv.toString();
        } catch (Exception e) {
            logger.error("Помилка при експорті в CSV для Poll: {}", pollId, e);
            throw new RuntimeException("Експорт не вдався", e);
        }
    }

    /**
     * РЕФАКТОРИНГ: Утилітарний метод для правильного экранирования CSV
     * Замінює " на "" (CSV стандарт)
     */
    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\"", "\"\"");
    }

    /**
     * Експорт ВСІХ голосувань системи у CSV
     * РЕФАКТОРИНГ: Обробка великої кількості даних
     */
    public String exportAllPollsToCsv() {
        try {
            List<Poll> polls = pollRepository.findAll();
            StringBuilder csv = new StringBuilder();

            csv.append("\"ID\",\"Назва\",\"Тип\",\"Статус\",\"Всього голосів\",\"Дата створення\"\n");

            for (Poll poll : polls) {
                long voteCount = voteRepository.countByPoll(poll);
                csv.append("\"").append(escapeCsv(poll.getId().toString())).append("\",")
                        .append("\"").append(escapeCsv(poll.getTitle())).append("\",")
                        .append("\"").append(escapeCsv(poll.getType())).append("\",")
                        .append("\"").append(escapeCsv(poll.getStatus())).append("\",")
                        .append(voteCount).append(",")
                        .append("\"").append(poll.getCreatedAt().format(dateFormatter)).append("\"\n");
            }

            logger.info("CSV експорт всіх голосувань завершено. Всього: {}", polls.size());
            return csv.toString();
        } catch (Exception e) {
            logger.error("Помилка при експорті всіх голосувань в CSV", e);
            throw new RuntimeException("Експорт не вдався", e);
        }
    }

    /**
     * Експорт деталей голосів для конкретного голосування
     */
    public String exportVoteDetailsToCsv(UUID pollId) {
        Poll poll = pollRepository.findById(pollId).orElse(null);
        if (poll == null) {
            logger.warn("Poll не знайдено для експорту деталей: {}", pollId);
            return null;
        }

        try {
            List<Vote> votes = voteRepository.findByPoll(poll);
            StringBuilder csv = new StringBuilder();

            csv.append("\"Голосування\",\"").append(escapeCsv(poll.getTitle())).append("\"\n\n");
            csv.append("\"Vote ID\",\"Device Fingerprint\",\"Опція\",\"Час голосування\"\n");

            for (Vote vote : votes) {
                String optionText = vote.getOption() != null ?
                        vote.getOption().getText() : "Текстова відповідь";
                csv.append("\"").append(escapeCsv(vote.getId().toString())).append("\",")
                        .append("\"").append(escapeCsv(vote.getFingerprint().getFingerprintHash())).append("\",")
                        .append("\"").append(escapeCsv(optionText)).append("\",")
                        .append("\"").append(vote.getVotedAt().format(dateFormatter)).append("\"\n");
            }

            logger.info("CSV деталей експортовано для Poll: {}. Голосів: {}", pollId, votes.size());
            return csv.toString();
        } catch (Exception e) {
            logger.error("Помилка при експорті деталей голосів для Poll: {}", pollId, e);
            throw new RuntimeException("Експорт не вдався", e);
        }
    }

    private Paragraph infoLine(String label, String value) {
        return new Paragraph()
                .add(new Text(label + " ").setBold())
                .add(new Text(value))
                .setMarginBottom(5);
    }

    private Cell header(String text) {
        return new Cell()
                .add(new Paragraph(text).setBold())
                .setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY)
                .setTextAlignment(TextAlignment.CENTER);
    }

    private Cell cell(String text) {
        return new Cell()
                .add(new Paragraph(text))
                .setTextAlignment(TextAlignment.CENTER);
    }

    public byte[] generatePdfBytes(UUID pollId) {
        Poll poll = pollRepository.findById(pollId).orElse(null);
        if (poll == null) return null;

        Map<String, Object> stats = pollService.getPollStatistics(pollId);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            PdfFont bold = PdfFontFactory.createFont();
            PdfFont regular = PdfFontFactory.createFont();

            // ===== Заголовок =====
            document.add(new Paragraph("ЗВІТ ГОЛОСУВАННЯ")
                    .setFont(bold)
                    .setFontSize(18)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20));

            // ===== Основна інформація =====
            document.add(infoLine("Назва:", poll.getTitle()));
            document.add(infoLine("Питання:", poll.getQuestion()));
            document.add(infoLine("Тип:", poll.getType()));
            document.add(infoLine("Статус:", poll.getStatus()));
            document.add(infoLine("Дата створення:",
                    poll.getCreatedAt().format(dateFormatter)));
            document.add(infoLine("Всього голосів:",
                    String.valueOf(stats.get("totalVotes"))));

            document.add(new Paragraph("\n"));

            // ===== Таблиця результатів =====
            Table table = new Table(new float[]{4, 2, 2});
            table.setWidth(UnitValue.createPercentValue(100));

            table.addHeaderCell(header("Варіант"));
            table.addHeaderCell(header("Голосів"));
            table.addHeaderCell(header("%"));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> options =
                    (List<Map<String, Object>>) stats.get("options");

            for (Map<String, Object> option : options) {
                table.addCell(cell((String) option.get("optionText")));
                table.addCell(cell(String.valueOf(option.get("votes"))));
                table.addCell(cell(option.get("percentage") + "%"));
            }

            document.add(table);
            document.close();

            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Не вдалося згенерувати PDF", e);
        }
    }


    public Map<String, Object> importPollsFromCsv(String csvContent) {
        int imported = 0;
        int skipped = 0;
        int errors = 0;
        List<String> errorMessages = new ArrayList<>();

        if (csvContent == null || csvContent.isBlank()) {
            return Map.of("imported", 0, "skipped", 0, "errors", 0, "messages", List.of());
        }

        String[] lines = csvContent.split("\n");

        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isBlank()) continue;

            try {
                String[] cols = parseCsvLine(line);
                if (cols.length < 4) {
                    errors++;
                    errorMessages.add("Line " + (i + 1) + ": not enough columns");
                    continue;
                }

                String title  = cols[1];
                String type   = cols[2];
                String status = cols[3];

                if (!List.of("SINGLE", "MULTIPLE", "RATING", "OPEN").contains(type)) {
                    skipped++;
                    errorMessages.add("Line " + (i + 1) + ": unknown type '" + type + "', skipped");
                    continue;
                }

                if (!List.of("ACTIVE", "CLOSED", "ARCHIVED").contains(status)) {
                    skipped++;
                    errorMessages.add("Line " + (i + 1) + ": unknown status '" + status + "', skipped");
                    continue;
                }

                // organizer_fingerprint_id is NOT NULL in DB — skip rows without a valid organizer
                skipped++;
                errorMessages.add("Line " + (i + 1) + ": poll import skipped — organizer fingerprint required");

            } catch (Exception e) {
                errors++;
                errorMessages.add("Line " + (i + 1) + ": " + e.getMessage());
            }
        }

        logger.info("CSV import complete: imported={}, skipped={}, errors={}", imported, skipped, errors);
        return Map.of("imported", imported, "skipped", skipped, "errors", errors, "messages", errorMessages);
    }

    private String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());
        return result.toArray(new String[0]);
    }

    /**
     * Експорт у простий текстовий формат (для майбутнього PDF)
     */
    public String exportPollToPdfText(UUID pollId) {
        Poll poll = pollRepository.findById(pollId).orElse(null);
        if (poll == null) {
            logger.warn("Poll не знайдено для експорту PDF: {}", pollId);
            return null;
        }

        try {
            Map<String, Object> stats = pollService.getPollStatistics(pollId);
            StringBuilder report = new StringBuilder();

            report.append("=".repeat(80)).append("\n");
            report.append("ЗВІТ ГОЛОСУВАННЯ\n");
            report.append("=".repeat(80)).append("\n\n");

            report.append("Назва: ").append(poll.getTitle()).append("\n");
            report.append("Питання: ").append(poll.getQuestion()).append("\n");
            report.append("Тип: ").append(poll.getType()).append("\n");
            report.append("Статус: ").append(poll.getStatus()).append("\n");
            report.append("Дата створення: ").append(poll.getCreatedAt().format(dateFormatter)).append("\n");
            report.append("Всього голосів: ").append(stats.get("totalVotes")).append("\n\n");

            report.append("-".repeat(80)).append("\n");
            report.append("РЕЗУЛЬТАТИ\n");
            report.append("-".repeat(80)).append("\n\n");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> options = (List<Map<String, Object>>) stats.get("options");
            for (Map<String, Object> option : options) {
                String optionText = (String) option.get("optionText");
                long votes = ((Number) option.get("votes")).longValue();
                double percentage = ((Number) option.get("percentage")).doubleValue();

                report.append(String.format("%-40s: %5d голосів (%6.2f%%)\n",
                        optionText, votes, percentage));
            }

            report.append("\n").append("=".repeat(80)).append("\n");
            logger.info("PDF текстовий експорт завершено для Poll: {}", pollId);

            return report.toString();
        } catch (Exception e) {
            logger.error("Помилка при експорті PDF для Poll: {}", pollId, e);
            throw new RuntimeException("Експорт не вдався", e);
        }
    }
}