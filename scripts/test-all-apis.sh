#!/bin/bash

# Load environment variables and logging functions
source ./scripts/env.sh

log_info "Starting API Test Suite for trustdeskAi..."
echo "--------------------------------------------------------------------------------"

# 1. Provisioning Phase
log_info "Step 1: Seeding sample orders..."
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/ingest-orders")
HTTP_STATUS=$(echo "$RESPONSE" | tail -n1)
if [ "$HTTP_STATUS" -eq 200 ]; then
    log_success "Orders seeded successfully."
else
    log_error "Failed to seed orders. Status: $HTTP_STATUS"
fi

log_info "Step 2: Testing bulk order ingestion..."
# Sample Order JSON
BULK_PAYLOAD='[
  {
    "orderId": "ORD-1001",
    "customerId": "CUST-001",
    "customerEmail": "test1@example.com",
    "purchaseDate": "2026-10-01T10:00:00",
    "deliveryDate": "2026-10-02T10:00:00",
    "orderStatus": "DELIVERED",
    "totalAmount": 150.50,
    "itemsJson": "[{\"item\": \"Gadget A\", \"qty\": 1}]"
  },
  {
    "orderId": "ORD-1002",
    "customerId": "CUST-002",
    "customerEmail": "test2@example.com",
    "purchaseDate": "2026-10-01T11:00:00",
    "deliveryDate": "2026-10-02T11:00:00",
    "orderStatus": "SHIPPED",
    "totalAmount": 89.99,
    "itemsJson": "[{\"item\": \"Widget B\", \"qty\": 2}]"
  }
]'

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/orders/bulk" \
    -H "Content-Type: application/json" \
    -d "$BULK_PAYLOAD")
HTTP_STATUS=$(echo "$RESPONSE" | tail -n1)
if [ "$HTTP_STATUS" -eq 200 ] || [ "$HTTP_STATUS" -eq 201 ]; then
    log_success "Bulk orders ingested successfully."
else
    log_error "Failed bulk order ingestion. Status: $HTTP_STATUS"
fi

log_info "Step 3: Ingesting AI policies..."
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/agent/ingest-policies")
HTTP_STATUS=$(echo "$RESPONSE" | tail -n1)
if [ "$HTTP_STATUS" -eq 200 ]; then
    log_success "Policies ingested successfully."
else
    log_error "Failed policy ingestion. Status: $HTTP_STATUS"
fi

# 2. Execution Phase
log_info "Step 4: Evaluating a support ticket..."
TICKET_PAYLOAD='{
  "ticketId": "TKT-555",
  "issueDescription": "My order ORD-1001 arrived broken. I would like a full refund based on the return policy."
}'

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/agent/evaluate-ticket" \
    -H "X-Conversation-Id: $CONVERSATION_ID" \
    -H "Content-Type: application/json" \
    -d "$TICKET_PAYLOAD")

# Separate body and status
BODY=$(echo "$RESPONSE" | sed '$d')
HTTP_STATUS=$(echo "$RESPONSE" | tail -n1)

if [ "$HTTP_STATUS" -eq 200 ]; then
    log_success "Ticket evaluated successfully."
    # Extract ticketId from response if possible (assuming JSON response like {"ticketId": "TKT-555", ...})
    # Using a simple grep for the ticketId pattern
    EXTRACTED_TICKET_ID=$(echo "$BODY" | grep -o '"ticketId":"[^"]*' | cut -d':' -f2 | tr -d '"')
    if [ -n "$EXTRACTED_TICKET_ID" ]; then
        log_info "Evaluated Ticket ID: $EXTRACTED_TICKET_ID"
    else
        EXTRACTED_TICKET_ID="TKT-555"
    fi
else
    log_error "Failed ticket evaluation. Status: $HTTP_STATUS"
fi

# 3. Verification Phase
log_info "Step 5: Checking pending approvals..."
RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/agent/pending-approvals")
HTTP_STATUS=$(echo "$RESPONSE" | tail -n1)
if [ "$HTTP_STATUS" -eq 200 ]; then
    log_success "Pending approvals list retrieved."
else
    log_error "Failed to retrieve pending approvals. Status: $HTTP_STATUS"
fi

log_info "Step 5b: Checking pending approvals for current conversation..."
RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/agent/pending-approvals/user" \
    -H "X-Conversation-Id: $CONVERSATION_ID")
HTTP_STATUS=$(echo "$RESPONSE" | tail -n1)
if [ "$HTTP_STATUS" -eq 200 ]; then
    log_success "Conversation-specific pending approvals retrieved."
else
    log_error "Failed to retrieve user pending approvals. Status: $HTTP_STATUS"
fi

# 4. Resolution Phase
log_info "Step 6: Submitting human review..."
REVIEW_PAYLOAD='{
  "approved": true,
  "overrideDecision": "Refund approved based on photo evidence of damage.",
  "reviewerNotes": "Customer is a gold member, expedite refund."
}'

# Use extracted ticket ID or fallback
TICKET_ID_TO_REVIEW=${EXTRACTED_TICKET_ID:-"TKT-555"}

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/agent/tickets/$TICKET_ID_TO_REVIEW/human-review" \
    -H "Content-Type: application/json" \
    -d "$REVIEW_PAYLOAD")
HTTP_STATUS=$(echo "$RESPONSE" | tail -n1)

if [ "$HTTP_STATUS" -eq 200 ] || [ "$HTTP_STATUS" -eq 201 ]; then
    log_success "Human review submitted successfully."
else
    log_error "Failed to submit human review. Status: $HTTP_STATUS"
fi

echo "--------------------------------------------------------------------------------"
log_info "API Test Suite Execution Complete."
