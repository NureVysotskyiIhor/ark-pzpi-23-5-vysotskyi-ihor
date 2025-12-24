package com.polls.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class IotSyncResponseDTO {
    private boolean success;
    private Map<String, Object> data;
    private String message;
}