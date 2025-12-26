package com.polls.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для повернення статистичних метрик розподілу голосів
 * РЕФАКТОРИНГ: МАТЕМАТИКА показники в окремому DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DistributionMetricsResponseDTO {

    // МАТЕМАТИКА: Середнє значення
    private Double mean;

    // МАТЕМАТИКА: Стандартне відхилення
    private Double stdDeviation;

    // МАТЕМАТИКА: Коефіцієнт варіації
    private Double coefficientOfVariation;

    // МАТЕМАТИКА: Мінімум та максимум
    private Double min;
    private Double max;

    // Для контексту
    private String pollId;
    private Long totalVotes;
}