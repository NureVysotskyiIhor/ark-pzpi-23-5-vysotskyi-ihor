package com.polls.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "iot_devices")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class IotDevice {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false, length = 100)
    private String kioskId;

    @Column(length = 255)
    private String location;

    @Column(length = 50)
    private String deviceType;

    @Column(nullable = false)
    private Boolean isActive;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime lastSync;

    private LocalDateTime updatedAt;

    // Constructors
    public IotDevice() {
        this.isActive = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public IotDevice(String kioskId, String location) {
        this();
        this.kioskId = kioskId;
        this.location = location;
        this.deviceType = "KIOSK";
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getKioskId() { return kioskId; }
    public void setKioskId(String kioskId) { this.kioskId = kioskId; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastSync() { return lastSync; }
    public void setLastSync(LocalDateTime lastSync) { this.lastSync = lastSync; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}