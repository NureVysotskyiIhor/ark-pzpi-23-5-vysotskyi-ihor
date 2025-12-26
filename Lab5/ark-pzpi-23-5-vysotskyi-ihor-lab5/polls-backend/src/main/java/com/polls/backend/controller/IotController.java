package com.polls.backend.controller;

import com.polls.backend.dto.*;
import com.polls.backend.entity.IotDevice;
import com.polls.backend.entity.IotDeviceConfig;
import com.polls.backend.entity.IotVote;
import com.polls.backend.service.IotVoteService;
import com.polls.backend.repository.IotDeviceRepository;
import com.polls.backend.repository.IotDeviceConfigRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/iot")
@Tag(name = "IoT", description = "Управління IoT киосками для голосування")
public class IotController {

    @Autowired
    private IotVoteService iotVoteService;

    @Autowired
    private IotDeviceRepository iotDeviceRepository;

    @Autowired
    private IotDeviceConfigRepository iotDeviceConfigRepository;

    /**
     * СИНХРОНИЗАЦИЯ киоска з сервером
     * GET /api/iot/sync/{kioskId}
     */
    @GetMapping("/sync/{deviceId}")
    @Operation(summary = "Синхронізувати конфіг киоска")
    public ResponseEntity<Map<String, Object>> syncDevice(@PathVariable String deviceId) {
        UUID deviceUUID = UUID.fromString(deviceId);
        Optional<IotDevice> deviceOpt = iotDeviceRepository.findById(deviceUUID);

        if (deviceOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "success", false,
                            "message", "IoT Device not found: " + deviceId
                    ));
        }

        IotDevice device = deviceOpt.get();
        device.setLastSync(LocalDateTime.now());
        iotDeviceRepository.save(device);

        Optional<IotDeviceConfig> configOpt = iotDeviceConfigRepository
                .findByIotDeviceId(device.getId());

        IotDeviceConfig config = configOpt.orElseGet(() -> {
            IotDeviceConfig newConfig = new IotDeviceConfig(device);
            return iotDeviceConfigRepository.save(newConfig);
        });

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);

        Map<String, Object> data = new HashMap<>();
        data.put("kioskId", device.getKioskId());
        data.put("location", device.getLocation());
        data.put("lastSync", device.getLastSync());

        Map<String, Object> configMap = new HashMap<>();
        configMap.put("pollIntervalMs", config.getPollIntervalMs());
        configMap.put("displayTimeoutMs", config.getDisplayTimeoutMs());
        configMap.put("confidenceThreshold", config.getConfidenceThreshold());
        configMap.put("anomalyThreshold", config.getAnomalyThreshold());
        configMap.put("isEnabled", config.getIsEnabled());
        configMap.put("configVersion", config.getConfigVersion());

        data.put("config", configMap);
        response.put("data", data);

        return ResponseEntity.ok(response);
    }

    /**
     * ОТПРАВКА голоса от киоска
     * POST /api/iot/votes
     */
    @PostMapping("/votes")
    @Operation(summary = "Зареєструвати IoT голос")
    public ResponseEntity<?> registerIotVote(@Valid @RequestBody CreateIotVoteRequestDTO request) {
        try {
            UUID iotDeviceId = UUID.fromString(request.getIotDeviceId());
            UUID pollId = UUID.fromString(request.getPollId());

            // Собрать данные голоса
            Map<String, Object> voteData = new HashMap<>();
            voteData.put("votingTimeMs", request.getVotingTimeMs());

            if (request.getOptionId() != null) {
                voteData.put("optionId", request.getOptionId());
            }
            if (request.getOptionIds() != null) {
                voteData.put("optionIds", request.getOptionIds());
            }
            if (request.getRating() != null) {
                voteData.put("rating", request.getRating());
            }
            if (request.getTextAnswer() != null) {
                voteData.put("textAnswer", request.getTextAnswer());
            }

            IotVote vote = iotVoteService.registerIotVote(iotDeviceId, pollId, voteData);

            if (vote == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Device or Poll not found"));
            }

            IotVoteResponseDTO response = new IotVoteResponseDTO();
            response.setVoteId(vote.getId().toString());
            response.setKioskId(vote.getIotDevice().getKioskId());
            response.setPollId(vote.getPoll().getId().toString());
            response.setPollType(vote.getPoll().getType());
            response.setVotingTimeMs((long) vote.getVotingTimeMs());
            response.setConfidence(vote.getConfidence());
            response.setAnomalyScore(vote.getAnomalyScore());
            response.setEntropy(vote.getEntropy());
            response.setValidationStatus(vote.getValidationStatus());
            response.setIsSuspicious(vote.getIsSuspicious());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid UUID format"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * СТАТИСТИКА киоска
     * GET /api/iot/devices/{kioskId}/stats
     */
    @GetMapping("/devices/{kioskId}/stats")
    @Operation(summary = "Отримати статистику киоска")
    public ResponseEntity<?> getDeviceStats(@PathVariable String kioskId) {
        Optional<IotDevice> deviceOpt = iotDeviceRepository.findByKioskId(kioskId);

        if (deviceOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> stats = iotVoteService.getDeviceStatistics(deviceOpt.get().getId());

        IotDeviceStatsDTO response = new IotDeviceStatsDTO();
        response.setKioskId(kioskId);
        response.setTotalVotes((Long) stats.get("totalVotes"));
        response.setApprovedVotes((Long) stats.get("approvedVotes"));
        response.setSuspiciousVotes((Long) stats.get("suspiciousVotes"));
        response.setApprovalRate((Float) stats.get("approvalRate"));

        return ResponseEntity.ok(response);
    }

    /**
     * ОБНОВИТЬ конфиг киоска (администратор)
     * PUT /api/iot/devices/{kioskId}/config
     */
    @PutMapping("/devices/{kioskId}/config")
    @Operation(summary = "Оновити конфіг киоска")
    public ResponseEntity<?> updateDeviceConfig(
            @PathVariable String kioskId,
            @RequestBody IotDeviceConfigDTO configDTO) {

        Optional<IotDevice> deviceOpt = iotDeviceRepository.findByKioskId(kioskId);

        if (deviceOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Optional<IotDeviceConfig> configOpt = iotDeviceConfigRepository
                .findByIotDeviceId(deviceOpt.get().getId());

        IotDeviceConfig config = configOpt.orElseGet(() ->
                new IotDeviceConfig(deviceOpt.get())
        );

        if (configDTO.getPollIntervalMs() != null) {
            config.setPollIntervalMs(configDTO.getPollIntervalMs());
        }
        if (configDTO.getDisplayTimeoutMs() != null) {
            config.setDisplayTimeoutMs(configDTO.getDisplayTimeoutMs());
        }
        if (configDTO.getConfidenceThreshold() != null) {
            config.setConfidenceThreshold(configDTO.getConfidenceThreshold());
        }
        if (configDTO.getAnomalyThreshold() != null) {
            config.setAnomalyThreshold(configDTO.getAnomalyThreshold());
        }
        if (configDTO.getIsEnabled() != null) {
            config.setIsEnabled(configDTO.getIsEnabled());
        }

        config.setConfigVersion(config.getConfigVersion() + 1);
        config.setUpdatedAt(LocalDateTime.now());

        IotDeviceConfig saved = iotDeviceConfigRepository.save(config);
        return ResponseEntity.ok(saved);
    }
}