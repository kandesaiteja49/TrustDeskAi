package com.demo.trustdeskAi.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
public class GuardrailService {

    // Common prompt injection patterns
    private static final List<Pattern> INJECTION_PATTERNS = List.of(
            Pattern.compile("(?i)ignore all previous instructions"),
            Pattern.compile("(?i)system override"),
            Pattern.compile("(?i)you are now a"),
            Pattern.compile("(?i)forget your rules"),
            Pattern.compile("(?i)reveal your secret prompt")
    );

    // PII Patterns to prevent sensitive data leakage
    private static final List<Pattern> PII_PATTERNS = List.of(
            Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}"), // Email
            Pattern.compile("\\b\\d{10,12}\\b"), // Simple Phone Number
            Pattern.compile("\\b(?:\\d[ -]*?){13,16}\\b") // Credit Card
    );

    // Forbidden promises in AI outputs
    private static final List<Pattern> FORBIDDEN_PROMISES = List.of(
            Pattern.compile("(?i)refund has been processed"),
            Pattern.compile("(?i)money has been sent"),
            Pattern.compile("(?i)already refunded"),
            Pattern.compile("(?i)guarantee a full refund")
    );

    public boolean checkPromptInjection(String input) {
        if (input == null || input.isBlank()) return false;

        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(input).find()) {
                log.warn("🚨 Prompt injection detected in input: {}", input);
                return true;
            }
        }
        return false;
    }

    public boolean containsPII(String input) {
        if (input == null || input.isBlank()) return false;

        for (Pattern pattern : PII_PATTERNS) {
            if (pattern.matcher(input).find()) {
                log.warn("🚨 PII detected in input: {}", input);
                return true;
            }
        }
        return false;
    }

    public boolean hasForbiddenPromises(String output) {
        if (output == null || output.isBlank()) return false;

        for (Pattern pattern : FORBIDDEN_PROMISES) {
            if (pattern.matcher(output).find()) {
                log.warn("🚨 Forbidden promise detected in AI output: {}", output);
                return true;
            }
        }
        return false;
    }

    public boolean isAdversarial(List<String> retrievedDocIds) {
        if (retrievedDocIds == null) return false;
        return retrievedDocIds.contains("KB-ADVERSARIAL-001");
    }
}
