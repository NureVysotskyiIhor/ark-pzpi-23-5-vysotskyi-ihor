package com.polls.backend.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для реєстрації нового адміністратора
 * РЕФАКТОРИНГ: Сильна валідація email та пароля
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateAdminRequestDTO {

    @NotBlank(message = "Email не може бути пустим")
    @Email(message = "Некоректна email адреса")
    private String email;

    @NotBlank(message = "Пароль не може бути пустим")
    @Size(min = 8, max = 255, message = "Пароль повинен містити від 8 до 255 символів")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
            message = "Пароль повинен містити великі, малі букви, цифри та спецсимволи"
    )
    private String password;

    @Size(min = 2, max = 100, message = "Ім'я повинно містити від 2 до 100 символів")
    private String name;
}