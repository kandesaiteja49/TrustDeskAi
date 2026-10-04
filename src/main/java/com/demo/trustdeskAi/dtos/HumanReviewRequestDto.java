package com.demo.trustdeskAi.dtos;

import lombok.Getter;

@Getter
public class HumanReviewRequestDto {
    private boolean approved;
    private String overrideDecision;
    private String reviewerNotes;
    private String idempotencyKey;


    // Default constructor
    public HumanReviewRequestDto() {}

    // Parameterized constructor
    public HumanReviewRequestDto(boolean approved, String overrideDecision, String reviewerNotes, String idempotencyKey) {
        this.approved = approved;
        this.overrideDecision = overrideDecision;
        this.reviewerNotes = reviewerNotes;
        this.idempotencyKey = idempotencyKey;
    }


    // Getters and Setters
    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }

    public String getOverrideDecision() {
        return overrideDecision;
    }

    public void setOverrideDecision(String overrideDecision) {
        this.overrideDecision = overrideDecision;
    }

    public String getReviewerNotes() {
        return reviewerNotes;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}
