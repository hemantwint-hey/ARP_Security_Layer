package org.example.arp_ecurity_layer.privacy.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Removes PII from free-text transaction remarks before forwarding to AI service.
 * Also strips prompt-injection patterns from user-submitted text.
 */
@Component
public class PiiScrubber {

    private static final Pattern EMAIL_PATTERN   = Pattern.compile("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}");
    private static final Pattern PHONE_PATTERN   = Pattern.compile("\\b[6-9]\\d{9}\\b");
    private static final Pattern ACCOUNT_PATTERN = Pattern.compile("\\b[A-Z]{2,4}\\d{8,16}\\b");
    private static final Pattern UPI_PATTERN     = Pattern.compile("[a-zA-Z0-9.\\-_]{3,}@[a-zA-Z]{3,}");
    private static final int     MAX_LENGTH      = 120;

    public record ScrubResult(String scrubbedText, boolean piiDetected, String fieldsStripped) {}

    /**
     * Removes PII from free-text transaction remarks.
     * Replaces emails, phone numbers, account numbers, UPI IDs with [REDACTED].
     * Truncates result to MAX_LENGTH characters.
     *
     * @param rawRemarks original transaction remarks (may contain PII)
     * @return ScrubResult with sanitized text and detection metadata
     */
    public ScrubResult scrubFreeText(String rawRemarks) {
        // TODO: Return ScrubResult("", false, "") for null/blank input
        // TODO: Apply EMAIL_PATTERN replacement -> "[REDACTED_EMAIL]", track in strippedFields list
        // TODO: Apply PHONE_PATTERN replacement -> "[REDACTED_PHONE]"
        // TODO: Apply ACCOUNT_PATTERN replacement -> "[REDACTED_ACCOUNT]"
        // TODO: Apply UPI_PATTERN replacement -> "[REDACTED_UPI]"
        // TODO: Convert strippedFields list to comma-separated string
        // TODO: Truncate scrubbedText to MAX_LENGTH
        // TODO: Return ScrubResult(scrubbedText, !strippedFields.isEmpty(), fieldsStripped)
        throw new UnsupportedOperationException("TODO: implement");
    }

    /**
     * Strips common prompt-injection phrases from user-submitted questions
     * before passing to LLM. Protects against: "ignore instructions",
     * "print customerId", "reveal token", "forget above", etc.
     *
     * @param input raw user input (e.g. AI advisor question text)
     * @return sanitized string safe for LLM context
     */
    public String stripPromptInjection(String input) {
        // TODO: Remove/replace phrases: "ignore", "forget", "disregard", "override instructions"
        // TODO: Remove: "print", "reveal", "show", "leak", "dump" followed by sensitive words
        // TODO: Strip control characters (ASCII < 32) and delimiter sequences (---, ===, ###)
        // TODO: Trim and return; preserve at least first 500 chars of legitimate query content
        throw new UnsupportedOperationException("TODO: implement");
    }
}

