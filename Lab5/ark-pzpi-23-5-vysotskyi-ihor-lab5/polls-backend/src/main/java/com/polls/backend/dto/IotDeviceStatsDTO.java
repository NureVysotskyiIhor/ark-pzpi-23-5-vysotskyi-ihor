package com.polls.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IotDeviceStatsDTO {
    private String kioskId;
    private Long totalVotes;
    private Long approvedVotes;
    private Long suspiciousVotes;
    private Float approvalRate;
}