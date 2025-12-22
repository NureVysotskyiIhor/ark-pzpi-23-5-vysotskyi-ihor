package com.polls.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class QrCodeService {

    private static final Logger logger = LoggerFactory.getLogger(QrCodeService.class);

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${app.poll-path:/vote}")
    private String pollPath;

    // ========================================================================
    // БІЗНЕС-ЛОГІКА: Генерування QR-коду з конфігуацією
    // ========================================================================

    /**
     * Генерування посилання для голосування
     * Використання конфіг замість hardcoded URL
     */
    public String generatePollLink(UUID pollId) {
        if (pollId == null) {
            throw new IllegalArgumentException("Poll ID не може бути null");
        }

        String link = baseUrl.replaceAll("/$", "") + pollPath + "/" + pollId;
        logger.debug("Посилання на голосування сгенеровано: {}", link);
        return link;
    }

    /**
     * Генерування QR-коду через Google Charts API
     * ВИПРАВЛЕННЯ: Правильне URL encoding (не Base64!)
     */
    public String generateQrCodeUrl(UUID pollId) {
        try {
            if (pollId == null) {
                throw new IllegalArgumentException("Poll ID не може бути null");
            }

            String pollLink = generatePollLink(pollId);

            // ✅ ВИПРАВЛЕНО: Правильне URL encoding для Google Charts
            String encodedLink = urlEncode(pollLink);

            String qrUrl = "https://chart.googleapis.com/chart?chs=300x300&chld=L|0&cht=qr&chl=" + encodedLink;
            logger.info("QR-код URL сгенеровано для Poll: {}", pollId);

            return qrUrl;
        } catch (Exception e) {
            logger.error("Помилка при генеруванні QR-коду для Poll: {}", pollId, e);
            throw new RuntimeException("Генерування QR-коду не вдалося", e);
        }
    }

    /**
     * Альтернативний варіант через qr-server.com (більш надійний)
     */
    public String generateQrCodeUrlAlternative(UUID pollId) {
        try {
            if (pollId == null) {
                throw new IllegalArgumentException("Poll ID не може бути null");
            }

            String pollLink = generatePollLink(pollId);
            String encodedLink = urlEncode(pollLink);

            // ✅ Більш надійна альтернатива
            String qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=" + encodedLink;
            logger.info("QR-код (alternative) сгенеровано для Poll: {}", pollId);

            return qrUrl;
        } catch (Exception e) {
            logger.error("Помилка при генеруванні QR-коду для Poll: {}", pollId, e);
            throw new RuntimeException("Генерування QR-коду не вдалося", e);
        }
    }

    /**
     * Генерування текстового представлення QR-коду
     */
    public String generateQrCodeAscii(UUID pollId) {
        if (pollId == null) {
            throw new IllegalArgumentException("Poll ID не може бути null");
        }

        return "QR-код для голосування: " + pollId + "\n" +
                "Посилання: " + generatePollLink(pollId) + "\n" +
                "Google Charts: " + generateQrCodeUrl(pollId);
    }

    /**
     * Отримати інформацію про QR для встановлення на стіну
     */
    public String getQrCodeInformation(UUID pollId) {
        if (pollId == null) {
            throw new IllegalArgumentException("Poll ID не може бути null");
        }

        StringBuilder info = new StringBuilder();
        info.append("╔════════════════════════════════════════════════════╗\n");
        info.append("║         QR-КОД ДЛЯ ГОЛОСУВАННЯ                    ║\n");
        info.append("╠════════════════════════════════════════════════════╣\n");
        info.append("║ ID голосування: ").append(pollId).append("\n");
        info.append("║ Посилання: ").append(generatePollLink(pollId)).append("\n");
        info.append("╠════════════════════════════════════════════════════╣\n");
        info.append("║ Google Charts API:                                 ║\n");
        info.append(generateQrCodeUrl(pollId)).append("\n");
        info.append("║                                                    ║\n");
        info.append("║ Альтернатива (qr-server.com):                     ║\n");
        info.append(generateQrCodeUrlAlternative(pollId)).append("\n");
        info.append("╚════════════════════════════════════════════════════╝\n");

        return info.toString();
    }

    /**
     * ✅ НОВА УТИЛІТА: Правильне URL encoding
     * Замінює спеціальні символи на %XX формат
     */
    private String urlEncode(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);

        for (byte b : bytes) {
            if ((b >= 'a' && b <= 'z') || (b >= 'A' && b <= 'Z') ||
                    (b >= '0' && b <= '9') || b == '-' || b == '_' ||
                    b == '.' || b == '~') {
                result.append((char) b);
            } else {
                result.append(String.format("%%%02X", b & 0xFF));
            }
        }

        return result.toString();
    }

    /**
     * Отримати детальну інформацію про QR-код
     */
    public QrCodeInfoDTO getQrCodeInfo(UUID pollId) {
        if (pollId == null) {
            throw new IllegalArgumentException("Poll ID не може бути null");
        }

        return QrCodeInfoDTO.builder()
                .pollId(pollId)
                .pollLink(generatePollLink(pollId))
                .qrCodeUrl(generateQrCodeUrl(pollId))
                .qrCodeUrlAlternative(generateQrCodeUrlAlternative(pollId))
                .build();
    }

    // ========================================================================
    // DTO для відповіді
    // ========================================================================

    public static class QrCodeInfoDTO {
        public UUID pollId;
        public String pollLink;
        public String qrCodeUrl;
        public String qrCodeUrlAlternative;

        public QrCodeInfoDTO(UUID pollId, String pollLink, String qrCodeUrl, String qrCodeUrlAlternative) {
            this.pollId = pollId;
            this.pollLink = pollLink;
            this.qrCodeUrl = qrCodeUrl;
            this.qrCodeUrlAlternative = qrCodeUrlAlternative;
        }

        public static QrCodeInfoDTOBuilder builder() {
            return new QrCodeInfoDTOBuilder();
        }

        public static class QrCodeInfoDTOBuilder {
            private UUID pollId;
            private String pollLink;
            private String qrCodeUrl;
            private String qrCodeUrlAlternative;

            public QrCodeInfoDTOBuilder pollId(UUID pollId) {
                this.pollId = pollId;
                return this;
            }

            public QrCodeInfoDTOBuilder pollLink(String pollLink) {
                this.pollLink = pollLink;
                return this;
            }

            public QrCodeInfoDTOBuilder qrCodeUrl(String qrCodeUrl) {
                this.qrCodeUrl = qrCodeUrl;
                return this;
            }

            public QrCodeInfoDTOBuilder qrCodeUrlAlternative(String qrCodeUrlAlternative) {
                this.qrCodeUrlAlternative = qrCodeUrlAlternative;
                return this;
            }

            public QrCodeInfoDTO build() {
                return new QrCodeInfoDTO(pollId, pollLink, qrCodeUrl, qrCodeUrlAlternative);
            }
        }
    }
}