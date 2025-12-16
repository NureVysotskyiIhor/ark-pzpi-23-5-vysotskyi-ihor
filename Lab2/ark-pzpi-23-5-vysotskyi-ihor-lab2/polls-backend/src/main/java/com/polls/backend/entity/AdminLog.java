package com.polls.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "admin_logs")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})

public class AdminLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_id", nullable = false)
    private Admin admin;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(nullable = false, length = 50)
    private String targetType;

    private UUID targetId;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Admin getAdmin() { return admin; }
    public void setAdmin(Admin admin) { this.admin = admin; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public UUID getTargetId() { return targetId; }
    public void setTargetId(UUID targetId) { this.targetId = targetId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
