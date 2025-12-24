package com.polls.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "polls")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "options", "votes"})
public class Poll {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String question;

    @Column(nullable = false, length = 20)
    private String type; // SINGLE, MULTIPLE, RATING, OPEN

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE"; // ACTIVE, CLOSED, ARCHIVED

    @Column(nullable = false)
    private Boolean multipleAnswers = false;

    @Column(nullable = false)
    private Boolean showResults = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime closedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closed_by_admin_id")
    private Admin closedByAdmin;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizer_fingerprint_id", nullable = false)
    private DeviceFingerprint organizerFingerprint;

    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PollOption> options;

    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Vote> votes;

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Boolean getMultipleAnswers() { return multipleAnswers; }
    public void setMultipleAnswers(Boolean multipleAnswers) { this.multipleAnswers = multipleAnswers; }

    public Boolean getShowResults() { return showResults; }
    public void setShowResults(Boolean showResults) { this.showResults = showResults; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }

    public Admin getClosedByAdmin() { return closedByAdmin; }
    public void setClosedByAdmin(Admin closedByAdmin) { this.closedByAdmin = closedByAdmin; }

    public DeviceFingerprint getOrganizerFingerprint() { return organizerFingerprint; }
    public void setOrganizerFingerprint(DeviceFingerprint organizerFingerprint) { this.organizerFingerprint = organizerFingerprint; }

    public List<PollOption> getOptions() { return options; }
    public void setOptions(List<PollOption> options) { this.options = options; }

    public List<Vote> getVotes() { return votes; }
    public void setVotes(List<Vote> votes) { this.votes = votes; }
}
