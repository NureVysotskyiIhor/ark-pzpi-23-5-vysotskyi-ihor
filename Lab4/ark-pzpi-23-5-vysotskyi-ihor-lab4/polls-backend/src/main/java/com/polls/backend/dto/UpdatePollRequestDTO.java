package com.polls.backend.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для оновлення голосування
 * РЕФАКТОРИНГ: Чітке розділення (create vs update)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePollRequestDTO {

    @Size(min = 3, max = 255, message = "Назва повинна містити від 3 до 255 символів")
    private String title;

    @Size(min = 5, max = 1000, message = "Питання повинно містити від 5 до 1000 символів")
    private String question;

    @Pattern(regexp = "^(ACTIVE|CLOSED|ARCHIVED)$",
            message = "Статус повинен бути: ACTIVE, CLOSED або ARCHIVED")
    private String status;

    private Boolean showResults;
}
