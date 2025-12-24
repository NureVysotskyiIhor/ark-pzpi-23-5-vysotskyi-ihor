package com.polls.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "votes")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Vote {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "poll_id", nullable = false)
    private Poll poll;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id")
    private PollOption option;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fingerprint_id", nullable = false)
    private DeviceFingerprint fingerprint;

    @Column(nullable = false, updatable = false)
    private LocalDateTime votedAt = LocalDateTime.now();

    @Column(columnDefinition = "text")
    private String textAnswer;

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Poll getPoll() { return poll; }
    public void setPoll(Poll poll) { this.poll = poll; }

    public PollOption getOption() { return option; }
    public void setOption(PollOption option) { this.option = option; }

    public DeviceFingerprint getFingerprint() { return fingerprint; }
    public void setFingerprint(DeviceFingerprint fingerprint) { this.fingerprint = fingerprint; }

    public LocalDateTime getVotedAt() { return votedAt; }
    public void setVotedAt(LocalDateTime votedAt) { this.votedAt = votedAt; }

    public String getTextAnswer() { return textAnswer; }
    public void setTextAnswer(String textAnswer) { this.textAnswer = textAnswer; }
}