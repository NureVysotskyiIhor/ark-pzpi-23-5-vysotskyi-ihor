package com.polls.backend.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IotSyncRequestDTO {
    @NotNull(message = "Kiosk ID не может быть null")
    private String kioskId;
}