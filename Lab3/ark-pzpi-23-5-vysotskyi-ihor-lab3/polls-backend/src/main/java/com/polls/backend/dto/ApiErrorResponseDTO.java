package com.polls.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO для стандартної відповіді про помилку
 * РЕФАКТОРИНГ: Консистентна структура помилок
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponseDTO {

    private String status;
    private String message;
    private LocalDateTime timestamp;
    private String path;

    // Для validation ошибок
    private Map<String, String> validationErrors;

    // Для внутрішніх ошибок
    private String errorCode;
}