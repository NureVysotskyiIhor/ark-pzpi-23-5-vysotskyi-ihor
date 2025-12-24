package com.polls.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "iot_votes")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class IotVote {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "iot_device_id", nullable = false)
    private IotDevice iotDevice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id", nullable = false)
    private Poll poll;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id")
    private PollOption option;

    @Column(columnDefinition = "text")
    private String optionIds;

    @Column
    private Integer rating;

    @Column(columnDefinition = "text")
    private String textAnswer;

    // МЕТРИКИ
    @Column(nullable = false)
    private Integer votingTimeMs;

    @Column
    private Float confidence;

    @Column
    private Float anomalyScore;

    @Column
    private Float entropy;

    @Column(nullable = false)
    private Boolean isSuspicious;

    @Column(length = 50)
    private String validationStatus;

    @Column(columnDefinition = "text")
    private String mathematicalAnalysis;

    @Column(columnDefinition = "text")
    private String deviceMetadata;

    @Column(nullable = false, updatable = false)
    private LocalDateTime votedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Constructors
    public IotVote() {
        this.isSuspicious = false;
        this.votedAt = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public IotDevice getIotDevice() { return iotDevice; }
    public void setIotDevice(IotDevice iotDevice) { this.iotDevice = iotDevice; }

    public Poll getPoll() { return poll; }
    public void setPoll(Poll poll) { this.poll = poll; }

    public PollOption getOption() { return option; }
    public void setOption(PollOption option) { this.option = option; }

    public String getOptionIds() { return optionIds; }
    public void setOptionIds(String optionIds) { this.optionIds = optionIds; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getTextAnswer() { return textAnswer; }
    public void setTextAnswer(String textAnswer) { this.textAnswer = textAnswer; }

    public Integer getVotingTimeMs() { return votingTimeMs; }
    public void setVotingTimeMs(Integer votingTimeMs) { this.votingTimeMs = votingTimeMs; }

    public Float getConfidence() { return confidence; }
    public void setConfidence(Float confidence) { this.confidence = confidence; }

    public Float getAnomalyScore() { return anomalyScore; }
    public void setAnomalyScore(Float anomalyScore) { this.anomalyScore = anomalyScore; }

    public Float getEntropy() { return entropy; }
    public void setEntropy(Float entropy) { this.entropy = entropy; }

    public Boolean getIsSuspicious() { return isSuspicious; }
    public void setIsSuspicious(Boolean isSuspicious) { this.isSuspicious = isSuspicious; }

    public String getValidationStatus() { return validationStatus; }
    public void setValidationStatus(String validationStatus) { this.validationStatus = validationStatus; }

    public String getMathematicalAnalysis() { return mathematicalAnalysis; }
    public void setMathematicalAnalysis(String mathematicalAnalysis) { this.mathematicalAnalysis = mathematicalAnalysis; }

    public String getDeviceMetadata() { return deviceMetadata; }
    public void setDeviceMetadata(String deviceMetadata) { this.deviceMetadata = deviceMetadata; }

    public LocalDateTime getVotedAt() { return votedAt; }
    public void setVotedAt(LocalDateTime votedAt) { this.votedAt = votedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}