package com.demo.trustdeskAi.controllers;

import com.demo.trustdeskAi.dtos.HumanReviewRequestDto;
import com.demo.trustdeskAi.dtos.TicketEvaluationResultDto;
import com.demo.trustdeskAi.dtos.TicketRequestDto;
import com.demo.trustdeskAi.dtos.TriageResultDto;
import com.demo.trustdeskAi.enitities.TicketEvaluationEntity;
import com.demo.trustdeskAi.service.EvaluationRunnerService;
import com.demo.trustdeskAi.service.MarkdownIngestionService;
import com.demo.trustdeskAi.service.TicketAgentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("agent")
public class TicketAgentController {

    private final MarkdownIngestionService ingestionService;
    private final TicketAgentService agentService;
    private final EvaluationRunnerService evaluationRunnerService;

    public TicketAgentController(MarkdownIngestionService ingestionService, TicketAgentService agentService, EvaluationRunnerService evaluationRunnerService) {
        this.ingestionService = ingestionService;
        this.agentService = agentService;
        this.evaluationRunnerService = evaluationRunnerService;
    }


    @PostMapping("/triage")
    public ResponseEntity<TriageResultDto> triageTicket(
            @RequestHeader("X-Conversation-Id") String conversationId,
            @RequestBody TicketRequestDto request) {
        TriageResultDto result = agentService.triageTicket(
                conversationId,
                request.getTicketId(),
                request.getIssueDescription()
        );
        return ResponseEntity.ok(result);
    }

    @PostMapping("/ingest-policies")

    public ResponseEntity<String> ingestPolicies() throws IOException {
        ingestionService.ingestAllPolicyMarkdownFiles();
        return ResponseEntity.ok("All policy Markdown files successfully ingested into PgVector.");
    }

//    @PostMapping("/evaluate-ticket")
//    public ResponseEntity<TicketEvaluationResultDto> evaluateTicket(
//            @RequestHeader("X-Conversation-Id") String conversationId,
//            @RequestBody TicketRequestDto request) {
//
//        TicketEvaluationResultDto result = agentService.evaluateTicket(
//                conversationId,
//                request.getTicketId(),
//                request.getIssueDescription()
//        );
//
//        return ResponseEntity.ok(result);
//    }

    @PostMapping("/evaluate-ticket")
    public ResponseEntity<TicketEvaluationResultDto> evaluateTicket(
            @RequestHeader("X-Conversation-Id") String conversationId,
            @RequestBody TicketRequestDto request) {

        // ❌ OLD: TicketEvaluationResultDto result = agentService.evaluateTicket(...);

        // ✅ NEW: Persist to DB so human review endpoints can find it
        TicketEvaluationResultDto result = agentService.evaluateAndPersistTicket(
                conversationId,
                request.getTicketId(),
                request.getIssueDescription()
        );

        return ResponseEntity.ok(result);
    }

    @GetMapping("/evaluate/run")
    public ResponseEntity<String> runEvaluation(@RequestParam String filePath) {
        String report = evaluationRunnerService.runEvaluation(filePath);
        return ResponseEntity.ok(report);
    }

    // 1. Get all tickets waiting for human review

    @GetMapping("/pending-approvals")
    public ResponseEntity<List<TicketEvaluationEntity>> getPendingApprovals() {
        return ResponseEntity.ok(agentService.getPendingHumanApprovals());
    }

    @GetMapping("/pending-approvals/user")
    public ResponseEntity<List<TicketEvaluationEntity>> getPendingApprovals(
            @RequestHeader("X-Conversation-Id") String conversationId) {

        List<TicketEvaluationEntity> pendingTickets = agentService.getPendingHumanApprovalsForConversation(conversationId);
        return ResponseEntity.ok(pendingTickets);
    }

    // 2. Submit human decision (Approve / Override)
    @PostMapping("/tickets/{ticketId}/human-review")
    public ResponseEntity<TicketEvaluationEntity> submitHumanReview(
            @PathVariable String ticketId,
            @RequestBody HumanReviewRequestDto reviewRequest) {

        TicketEvaluationEntity updatedTicket = agentService.processHumanReview(
                ticketId,
                reviewRequest.isApproved(),
                reviewRequest.getOverrideDecision(),
                reviewRequest.getReviewerNotes(),
                reviewRequest.getIdempotencyKey()
        );

        return ResponseEntity.ok(updatedTicket);
    }
}