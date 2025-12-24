package com.polls.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "device_fingerprints")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})

public class DeviceFingerprint {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false, length = 255)
    private String fingerprintHash;

    @Column(columnDefinition = "inet", name = "ip")
    private String ip;

    @Column(columnDefinition = "text")
    private String userAgent;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime lastSeen = LocalDateTime.now();

    @Column(nullable = false)
    private Boolean isBlocked = false;

    @Column(columnDefinition = "text")
    private String blockReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_by_admin_id")
    private Admin blockedByAdmin;

    private LocalDateTime blockedAt;

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getFingerprintHash() { return fingerprintHash; }
    public void setFingerprintHash(String fingerprintHash) { this.fingerprintHash = fingerprintHash; }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastSeen() { return lastSeen; }
    public void setLastSeen(LocalDateTime lastSeen) { this.lastSeen = lastSeen; }

    public Boolean getIsBlocked() { return isBlocked; }
    public void setIsBlocked(Boolean isBlocked) { this.isBlocked = isBlocked; }

    public String getBlockReason() { return blockReason; }
    public void setBlockReason(String blockReason) { this.blockReason = blockReason; }

    public Admin getBlockedByAdmin() { return blockedByAdmin; }
    public void setBlockedByAdmin(Admin blockedByAdmin) { this.blockedByAdmin = blockedByAdmin; }

    public LocalDateTime getBlockedAt() { return blockedAt; }
    public void setBlockedAt(LocalDateTime blockedAt) { this.blockedAt = blockedAt; }
}