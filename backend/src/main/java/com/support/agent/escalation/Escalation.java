package com.support.agent.escalation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;

@Entity
public class Escalation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String conversationId;
    @Column(columnDefinition = "text")
    private String question;
    private double confidence;
    private String status = "OPEN";
    private Instant createdAt = Instant.now();

    protected Escalation() {}

    public Escalation(String conversationId, String question, double confidence) {
        this.conversationId = conversationId;
        this.question = question;
        this.confidence = confidence;
    }

    public Long getId() { return id; }
    public String getConversationId() { return conversationId; }
    public String getQuestion() { return question; }
    public double getConfidence() { return confidence; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
