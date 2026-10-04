package com.demo.trustdeskAi.service;



import com.demo.trustdeskAi.dtos.TicketEvaluationResultDto;
import com.demo.trustdeskAi.dtos.TriageResultDto;
import com.demo.trustdeskAi.enitities.AITraceEntity;
import com.demo.trustdeskAi.enitities.TicketEvaluationEntity;
import com.demo.trustdeskAi.repositories.AITraceRepository;
import com.demo.trustdeskAi.repositories.TicketEvaluationRepository;
import com.demo.trustdeskAi.utils.enums.TicketStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketAgentService {

    private final ChatClient chatClient;
    private final TicketEvaluationRepository ticketRepository;
    private final AITraceRepository traceRepository;
    private final GuardrailService guardrailService;


    // Inject the single pre-configured ChatClient instance
//    public TicketAgentService(ChatClient chatClient) {
//        this.chatClient = chatClient;
//    }

    public TicketEvaluationResultDto evaluateTicket(String conversationId, String ticketId, String issueDescription) {

        log.info("Evaluating ticket with ID: {} and issue: {}", ticketId, issueDescription);
        return chatClient.prompt()
                .user(String.format("Ticket ID: %s\nIssue: %s", ticketId, issueDescription))
                .advisors(spec -> spec.param("chat_memory_conversation_id", conversationId))
                .call()
                .entity(TicketEvaluationResultDto.class);
    }

    @Transactional(readOnly = true)
    public List<TicketEvaluationEntity> getPendingHumanApprovals() {
        log.info("Fetching all tickets pending human approval");
        return ticketRepository.findByStatusOrderByCreatedAtDesc(TicketStatus.PENDING_HUMAN_APPROVAL);
    }

    @Transactional(readOnly = true)
    public List<TicketEvaluationEntity> getPendingHumanApprovalsForConversation(String conversationId) {
        log.info("Fetching tickets pending human approval for conversation ID: {}", conversationId);
        return ticketRepository.findByStatusAndConversationIdOrderByCreatedAtDesc(
                TicketStatus.PENDING_HUMAN_APPROVAL,
                conversationId
        );
    }

    @Transactional
    public TicketEvaluationEntity processHumanReview(String ticketId, boolean approved, String overrideDecision, String reviewerNotes, String idempotencyKey) {
        log.info("Processing human review for ticket ID: {}. Approved: {}", ticketId, approved);

        TicketEvaluationEntity ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new NoSuchElementException("Ticket not found with ID: " + ticketId));

        // Idempotency Check
        if (idempotencyKey != null && idempotencyKey.equals(ticket.getLastIdempotencyKey())) {
            log.info("Duplicate request detected for ticket [{}] with idempotency key [{}]. Returning existing result.", ticketId, idempotencyKey);
            return ticket;
        }

        if (ticket.getStatus() != TicketStatus.PENDING_HUMAN_APPROVAL) {
            throw new IllegalStateException("Ticket " + ticketId + " is not pending human approval. Current status: " + ticket.getStatus());
        }

        if (approved) {
            ticket.setStatus(TicketStatus.APPROVED_BY_HUMAN);
            ticket.setFinalDecision(ticket.getProposedDecision());
            log.info("Human reviewer APPROVED AI proposed decision for ticket [{}]", ticketId);
        } else if (overrideDecision != null && !overrideDecision.isBlank()) {
            ticket.setStatus(TicketStatus.OVERRIDDEN_BY_HUMAN);
            ticket.setFinalDecision(overrideDecision);
            log.info("Human reviewer OVERRODE decision for ticket [{}] with: {}", ticketId, overrideDecision);
        } else {
            ticket.setStatus(TicketStatus.REJECTED);
            ticket.setFinalDecision("REJECTED: " + (reviewerNotes != null ? reviewerNotes : "No notes provided"));
            log.info("Human reviewer REJECTED AI proposal for ticket [{}]", ticketId);
        }

        ticket.setHumanReviewerNotes(reviewerNotes);
        ticket.setReviewedAt(LocalDateTime.now());
        ticket.setLastIdempotencyKey(idempotencyKey);

        return ticketRepository.save(ticket);
    }

    @Transactional(readOnly = true)
    public TriageResultDto triageTicket(String conversationId, String ticketId, String issueDescription) {
        log.info("Triaging ticket ID: {}", ticketId);
        return chatClient.prompt()
                .user(String.format(
                    "Classify the following customer support ticket into one of these categories: shipping, refund, warranty, billing, account_security, general. " +
                    "Assign a priority: low, medium, high, urgent. " +
                    "Determine if immediate human escalation is needed. " +
                    "Ticket ID: %s\nIssue: %s", ticketId, issueDescription))
                .advisors(spec -> spec.param("chat_memory_conversation_id", conversationId))
                .call()
                .entity(TriageResultDto.class);
    }

    @Transactional
    public TicketEvaluationResultDto evaluateAndPersistTicket(String conversationId, String ticketId, String issueDescription) {

        AITraceEntity trace = AITraceEntity.builder()
                .ticketId(ticketId)
                .timestamp(LocalDateTime.now())
                .build();

        // 1. Guardrail Check - Input
        if (guardrailService.checkPromptInjection(issueDescription)) {
            log.error("🚨 Security violation: Prompt injection detected for ticket [{}]", ticketId);
            trace.setGuardrailResult("BLOCKED: Prompt Injection");
            trace.setFinalStatus("BLOCKED");
            traceRepository.save(trace);
            throw new SecurityException("Prompt injection detected. Request blocked.");
        }
        if (guardrailService.containsPII(issueDescription)) {
            log.error("🚨 Security violation: PII detected for ticket [{}]", ticketId);
            trace.setGuardrailResult("BLOCKED: PII Detected");
            trace.setFinalStatus("BLOCKED");
            traceRepository.save(trace);
            throw new SecurityException("Sensitive PII detected in input. Request blocked.");
        }
        trace.setGuardrailResult("PASSED");

        // 2. Evaluate with Gemini + PgVector
        TicketEvaluationResultDto result = chatClient.prompt()
                .user(String.format(
                    "Ticket ID: %s\nIssue: %s\n\n" +
                    "Evaluate this ticket based on retrieved policies. " +
                    "Generate a professional customer-facing reply. " +
                    "CRITICAL: For every claim made in the reply, you MUST include a citation ID from the provided policy documents in the format [KB-XXXX]. " +
                    "If no document supports a claim, do not cite it.",
                    ticketId, issueDescription))
                .advisors(spec -> spec.param("chat_memory_conversation_id", conversationId))
                .call()
                .entity(TicketEvaluationResultDto.class);

        // 3. Guardrail Check - Output
        if (result != null && result.getDraftReply() != null && guardrailService.hasForbiddenPromises(result.getDraftReply())) {
            log.warn("⚠️ Guardrail violation: AI promised prohibited action for ticket [{}]", ticketId);
            trace.setGuardrailResult("FLAGGED: Forbidden Promise");
            // We don't block the entire response but flag it for mandatory human review
            result.setRequiresHumanApproval(true);
            result.setReasoning(result.getReasoning() + " [GUARDRAIL: AI made a forbidden promise in the draft]");
        }

        // 4. Handle Adversarial Documents
        if (result != null && result.getCitedPolicyDoc() != null && result.getCitedPolicyDoc().contains("KB-ADVERSARIAL-001")) {
            log.warn("⚠️ Adversarial document KB-ADVERSARIAL-001 cited in ticket [{}]", ticketId);
            trace.setGuardrailResult("FLAGGED: Adversarial Doc");
        }

        // 5. Map to Entity & Determine Initial Status
        TicketEvaluationEntity entity = new TicketEvaluationEntity();

        entity.setTicketId(Objects.requireNonNull(result).getTicketId());
        entity.setConversationId(conversationId);
        entity.setIssueDescription(issueDescription);
        entity.setProposedDecision(result.getDecision());
        entity.setPriority(result.getPriority());
        entity.setReasoning(result.getReasoning());
        entity.setRequiresHumanApproval(result.isRequiresHumanApproval());
        entity.setCitedPolicyDoc(result.getCitedPolicyDoc());
        entity.setDraftReply(result.getDraftReply());
        entity.setCreatedAt(LocalDateTime.now());

        if (result.isRequiresHumanApproval()) {
            entity.setStatus(TicketStatus.PENDING_HUMAN_APPROVAL);
            log.warn("🚨 Ticket [{}] flagged for human review. Priority: {}", ticketId, result.getPriority());
        } else {
            entity.setStatus(TicketStatus.AUTO_RESOLVED);
            entity.setFinalDecision(result.getDecision());
            log.info("✅ Ticket [{}] auto-resolved.", ticketId);
        }

        ticketRepository.save(entity);

        // 6. Complete Trace
        trace.setFinalStatus(entity.getStatus().name());
        trace.setRetrievedDocIds(result.getCitedPolicyDoc());
        traceRepository.save(trace);

        return result;
    }


    public record TicketEvaluationResult(
            String ticketId,
            String decision,
            String priority,
            String reasoning,
            boolean requiresHumanApproval,
            String citedPolicyDoc
    ) {}
}