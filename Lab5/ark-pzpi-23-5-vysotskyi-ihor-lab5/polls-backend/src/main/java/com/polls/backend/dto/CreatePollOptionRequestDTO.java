package com.polls.backend.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для додавання варіанту відповіді
 * РЕФАКТОРИНГ: Валідація orderNum та text
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePollOptionRequestDTO {

    @NotNull(message = "Poll ID не може бути null")
    private String pollId;

    @NotBlank(message = "Текст варіанту не може бути пустим")
    @Size(min = 1, max = 500, message = "Текст повинен містити від 1 до 500 символів")
    private String text;

    @PositiveOrZero(message = "orderNum повинен бути >= 0")
    @NotNull(message = "orderNum не може бути null")
    private Integer orderNum;
}