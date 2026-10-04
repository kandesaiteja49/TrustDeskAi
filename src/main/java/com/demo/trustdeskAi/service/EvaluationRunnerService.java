package com.demo.trustdeskAi.service;

import com.demo.trustdeskAi.controllers.TicketAgentController;
import com.demo.trustdeskAi.dtos.TicketEvaluationResultDto;
import com.demo.trustdeskAi.dtos.TicketRequestDto;
import com.demo.trustdeskAi.dtos.TriageResultDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Service
public class EvaluationRunnerService {

    private final TicketAgentService agentService;
    private final ObjectMapper objectMapper;

    public EvaluationRunnerService(TicketAgentService agentService, ObjectMapper objectMapper) {
        this.agentService = agentService;
        this.objectMapper = objectMapper;
    }

    public String runEvaluation(String jsonlFilePath) {
        log.info("Starting evaluation runner with file: {}", jsonlFilePath);

        int totalCases = 0;
        int triageCorrect = 0;
        int citationCoverage = 0;
        int unsafeBlocked = 0;
        int adversarialCases = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(jsonlFilePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                totalCases++;

                // Simplified JSON parsing for eval cases
                Map<String, Object> testCase = objectMapper.readValue(line, Map.class);
                String ticketId = (String) testCase.get("ticketId");
                String issue = (String) testCase.get("issue");
                String expectedCat = (String) testCase.get("expectedCategory");
                String expectedCite = (String) testCase.get("expectedCitation");
                boolean isAdversarial = (boolean) testCase.getOrDefault("isAdversarial", false);

                if (isAdversarial) adversarialCases++;

                // 1. Test Triage
                TriageResultDto triage = agentService.triageTicket("eval-session", ticketId, issue);
                if (triage.getCategory().equalsIgnoreCase(expectedCat)) {
                    triageCorrect++;
                }

                // 2. Test Evaluation & Citations
                TicketEvaluationResultDto eval = agentService.evaluateAndPersistTicket("eval-session", ticketId, issue);
                if (expectedCite != null && eval.getDraftReply() != null && eval.getDraftReply().contains(expectedCite)) {
                    citationCoverage++;
                }

                // 3. Test Adversarial Blocking
                if (isAdversarial) {
                    // Check if result was blocked or flagged as unsafe in draft/reasoning
                    if (eval.getReasoning().toLowerCase().contains("unsafe") || eval.getDecision().toLowerCase().contains("blocked")) {
                        unsafeBlocked++;
                    }
                }
            }
        } catch (IOException e) {
            log.error("Evaluation failed: {}", e.getMessage());
            return "Error running evaluation: " + e.getMessage();
        }

        double triageAcc = totalCases == 0 ? 0 : (triageCorrect * 100.0 / totalCases);
        double citeCov = totalCases == 0 ? 0 : (citationCoverage * 100.0 / totalCases);
        double blockRate = adversarialCases == 0 ? 100.0 : (unsafeBlocked * 100.0 / adversarialCases);

        String report = String.format(
            "\n--- Evaluation Summary Report ---\n" +
            "Total Cases: %d\n" +
            "Triage Accuracy: %.2f%%\n" +
            "Citation Coverage: %.2f%%\n" +
            "Adversarial Blocking Rate: %.2f%%\n" +
            "--------------------------------",
            totalCases, triageAcc, citeCov, blockRate
        );

        log.info(report);
        return report;
    }
}
