package com.polls.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IotDeviceConfigDTO {
    private Integer pollIntervalMs;
    private Integer displayTimeoutMs;
    private Float confidenceThreshold;
    private Float anomalyThreshold;
    private Boolean isEnabled;
    private Integer configVersion;
}