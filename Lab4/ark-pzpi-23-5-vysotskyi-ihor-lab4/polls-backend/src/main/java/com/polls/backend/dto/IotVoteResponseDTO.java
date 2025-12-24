package com.polls.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IotVoteResponseDTO {
    private String voteId;
    private String kioskId;
    private String pollId;
    private String pollType;
    private Long votingTimeMs;
    private Float confidence;
    private Float anomalyScore;
    private Float entropy;
    private String validationStatus;
    private Boolean isSuspicious;
}