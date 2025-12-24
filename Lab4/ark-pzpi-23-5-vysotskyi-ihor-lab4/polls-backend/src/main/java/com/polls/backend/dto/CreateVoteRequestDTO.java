package com.polls.backend.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для реєстрації голосу
 * РЕФАКТОРИНГ: Обов'язкова валідація IDs та опції
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateVoteRequestDTO {

    @NotNull(message = "Poll ID не може бути null")
    private String pollId;

    @NotNull(message = "Device Fingerprint ID не може бути null")
    private String fingerprintId;

    // optionId может быть null для OPEN типу голосувань
    private String optionId;

    @Size(max = 1000, message = "Текстова відповідь не повинна перевищувати 1000 символів")
    private String textAnswer;

    /**
     * РЕФАКТОРИНГ: Кастомна валідація
     * Перевіримо що або optionId або textAnswer присутні
     */
    @AssertTrue(message = "Повинна бути обрана опція або надана текстова відповідь")
    public boolean isValidVote() {
        // Якщо нема optionId, то повинен бути textAnswer
        return optionId != null || (textAnswer != null && !textAnswer.trim().isEmpty());
    }
}