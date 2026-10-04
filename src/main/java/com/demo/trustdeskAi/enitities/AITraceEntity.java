package com.demo.trustdeskAi.enitities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "ai_traces")
public class AITraceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ticketId;
    private LocalDateTime timestamp;

    @Column(columnDefinition = "TEXT")
    private String retrievedDocIds;

    @Column(columnDefinition = "TEXT")
    private String toolCalls;

    private String guardrailResult;

    private String finalStatus;
}
