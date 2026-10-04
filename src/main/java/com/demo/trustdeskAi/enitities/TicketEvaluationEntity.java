package com.demo.trustdeskAi.enitities;


import com.demo.trustdeskAi.utils.enums.TicketStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "ticket_evaluations")
public class TicketEvaluationEntity {

    @Id
    private String ticketId;

    private String conversationId;
    private String issueDescription;
    private String proposedDecision;
    private String priority;

    @Column(columnDefinition = "TEXT")
    private String reasoning;

    private boolean requiresHumanApproval;
    private String citedPolicyDoc;
    @Column(columnDefinition = "TEXT")
    private String draftReply;


    @Enumerated(EnumType.STRING)
    private TicketStatus status;

    private String humanReviewerNotes;
    private String finalDecision;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
    private String lastIdempotencyKey;


    // Getters, Setters, Constructors...
}