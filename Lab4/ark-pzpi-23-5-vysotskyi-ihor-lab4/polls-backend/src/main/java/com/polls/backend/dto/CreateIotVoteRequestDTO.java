
package com.polls.backend.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateIotVoteRequestDTO {
    @NotNull(message = "IoT Device ID не может быть null")
    private String iotDeviceId;

    @NotNull(message = "Poll ID не может быть null")
    private String pollId;

    @NotNull(message = "Voting time не может быть null")
    private Long votingTimeMs;

    // Для SINGLE
    private String optionId;

    // Для MULTIPLE
    private String optionIds;

    // Для RATING
    private Integer rating;

    // Для OPEN
    private String textAnswer;
}