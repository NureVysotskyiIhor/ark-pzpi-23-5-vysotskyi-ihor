package com.polls.backend.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для блокування device fingerprint адміністратором
 * РЕФАКТОРИНГ: Причина блокування є обов'язковою
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlockDeviceRequestDTO {

    @NotNull(message = "Device Fingerprint ID не може бути null")
    private String fingerprintId;

    @NotBlank(message = "Причина блокування не може бути пустою")
    @Size(min = 5, max = 500, message = "Причина повинна містити від 5 до 500 символів")
    private String blockReason;
}