package com.polls.backend.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для запиту створення голосування
 * РЕФАКТОРИНГ: Валідація на рівні DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePollRequestDTO {

    @NotBlank(message = "Назва голосування не може бути пустою")
    @Size(min = 3, max = 255, message = "Назва повинна містити від 3 до 255 символів")
    private String title;

    @NotBlank(message = "Питання не може бути пустим")
    @Size(min = 5, max = 1000, message = "Питання повинно містити від 5 до 1000 символів")
    private String question;

    @NotNull(message = "Тип голосування не може бути null")
    @Pattern(regexp = "^(SINGLE|MULTIPLE|RATING|OPEN)$",
            message = "Тип повинен бути: SINGLE, MULTIPLE, RATING або OPEN")
    private String type;

    @NotNull(message = "multipleAnswers не може бути null")
    private Boolean multipleAnswers;

    @NotNull(message = "showResults не може бути null")
    private Boolean showResults;

    @NotNull(message = "Отримувач (fingerprint) не може бути null")
    private String organizerFingerprintId;
}