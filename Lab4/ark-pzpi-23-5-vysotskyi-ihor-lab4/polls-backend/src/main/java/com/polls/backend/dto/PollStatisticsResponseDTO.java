package com.polls.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO для повернення статистики голосування
 * РЕФАКТОРИНГ: Структурована відповідь
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PollStatisticsResponseDTO {

    private String pollId;
    private String pollTitle;
    private String status;
    private LocalDateTime createdAt;
    private Long totalVotes;

    // МАТЕМАТИКА: Список варіантів з голосами та відсотками
    private List<OptionStatistics> options;

    // МАТЕМАТИКА: Лідер за голосами
    private OptionStatistics leader;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionStatistics {
        private String optionId;
        private String optionText;
        private Long votes;
        private Double percentage;
        private Integer order;
    }
}