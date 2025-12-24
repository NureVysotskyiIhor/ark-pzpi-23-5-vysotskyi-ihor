package com.polls.backend.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для розблокування device fingerprint адміністратором
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnBlockDeviceRequestDTO {

    @NotNull(message = "Device Fingerprint ID не може бути null")
    private String fingerprintId;
}