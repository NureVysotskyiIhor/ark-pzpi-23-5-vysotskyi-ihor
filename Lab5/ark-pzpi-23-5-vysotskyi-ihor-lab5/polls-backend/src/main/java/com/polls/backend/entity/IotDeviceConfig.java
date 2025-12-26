package com.polls.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "iot_device_configs")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class IotDeviceConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "iot_device_id", nullable = false)
    private IotDevice iotDevice;

    @Column(nullable = false)
    private Integer pollIntervalMs;

    @Column(nullable = false)
    private Integer displayTimeoutMs;

    @Column(nullable = false)
    private Float confidenceThreshold;

    @Column(nullable = false)
    private Float anomalyThreshold;

    @Column(nullable = false)
    private Boolean isEnabled;

    @Column(nullable = false)
    private Float smoothingAlpha;

    @Column(nullable = false)
    private Integer configVersion;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // Constructors
    public IotDeviceConfig() {
        this.pollIntervalMs = 5000;
        this.displayTimeoutMs = 30000;
        this.confidenceThreshold = 0.6f;
        this.anomalyThreshold = 2.5f;
        this.isEnabled = true;
        this.smoothingAlpha = 0.1f;
        this.configVersion = 1;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public IotDeviceConfig(IotDevice iotDevice) {
        this();
        this.iotDevice = iotDevice;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public IotDevice getIotDevice() { return iotDevice; }
    public void setIotDevice(IotDevice iotDevice) { this.iotDevice = iotDevice; }

    public Integer getPollIntervalMs() { return pollIntervalMs; }
    public void setPollIntervalMs(Integer pollIntervalMs) { this.pollIntervalMs = pollIntervalMs; }

    public Integer getDisplayTimeoutMs() { return displayTimeoutMs; }
    public void setDisplayTimeoutMs(Integer displayTimeoutMs) { this.displayTimeoutMs = displayTimeoutMs; }

    public Float getConfidenceThreshold() { return confidenceThreshold; }
    public void setConfidenceThreshold(Float confidenceThreshold) { this.confidenceThreshold = confidenceThreshold; }

    public Float getAnomalyThreshold() { return anomalyThreshold; }
    public void setAnomalyThreshold(Float anomalyThreshold) { this.anomalyThreshold = anomalyThreshold; }

    public Boolean getIsEnabled() { return isEnabled; }
    public void setIsEnabled(Boolean isEnabled) { this.isEnabled = isEnabled; }

    public Float getSmoothingAlpha() { return smoothingAlpha; }
    public void setSmoothingAlpha(Float smoothingAlpha) { this.smoothingAlpha = smoothingAlpha; }

    public Integer getConfigVersion() { return configVersion; }
    public void setConfigVersion(Integer configVersion) { this.configVersion = configVersion; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}