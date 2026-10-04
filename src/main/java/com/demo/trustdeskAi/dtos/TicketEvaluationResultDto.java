package com.demo.trustdeskAi.dtos;

public class TicketEvaluationResultDto {
    private String ticketId;
    private String decision;
    private String priority;
    private String reasoning;
    private boolean requiresHumanApproval;
    private String citedPolicyDoc;
    private String draftReply;


    // Default constructor
    public TicketEvaluationResultDto() {}

    // Parameterized constructor
    public TicketEvaluationResultDto(String ticketId, String decision, String priority,
                                     String reasoning, boolean requiresHumanApproval,
                                     String citedPolicyDoc) {
        this.ticketId = ticketId;
        this.decision = decision;
        this.priority = priority;
        this.reasoning = reasoning;
        this.requiresHumanApproval = requiresHumanApproval;
        this.citedPolicyDoc = citedPolicyDoc;
    }

    // Getters and Setters
    public String getTicketId() {
        return ticketId;
    }


    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getReasoning() {
        return reasoning;
    }

    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }

    public boolean isRequiresHumanApproval() {
        return requiresHumanApproval;
    }

    public void setRequiresHumanApproval(boolean requiresHumanApproval) {
        this.requiresHumanApproval = requiresHumanApproval;
    }

    public String getCitedPolicyDoc() {
        return citedPolicyDoc;
    }

    public String getDraftReply() {
        return draftReply;
    }

    public void setDraftReply(String draftReply) {
        this.draftReply = draftReply;
    }
}
