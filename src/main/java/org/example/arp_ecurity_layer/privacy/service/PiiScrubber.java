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
        if (rawRemarks == null || rawRemarks.isBlank()) {
            return new ScrubResult("", false, "");
        }

        String text = rawRemarks;
        List<String> stripped = new ArrayList<>();

        if (EMAIL_PATTERN.matcher(text).find()) {
            text = EMAIL_PATTERN.matcher(text).replaceAll("[REDACTED_EMAIL]");
            stripped.add("EMAIL");
        }
        if (PHONE_PATTERN.matcher(text).find()) {
            text = PHONE_PATTERN.matcher(text).replaceAll("[REDACTED_PHONE]");
            stripped.add("PHONE");
        }
        if (ACCOUNT_PATTERN.matcher(text).find()) {
            text = ACCOUNT_PATTERN.matcher(text).replaceAll("[REDACTED_ACCOUNT]");
            stripped.add("ACCOUNT_NO");
        }
        if (UPI_PATTERN.matcher(text).find()) {
            text = UPI_PATTERN.matcher(text).replaceAll("[REDACTED_UPI]");
            stripped.add("UPI_ID");
        }

        if (text.length() > MAX_LENGTH) {
            text = text.substring(0, MAX_LENGTH);
        }

        return new ScrubResult(text.trim(), !stripped.isEmpty(), String.join(",", stripped));
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
    if (input == null || input.isBlank()) {
        return "";
    }

    // Convert to lowercase to handle the "(?i)" case-insensitivity part
    String sanitized = input.toLowerCase();

    // 1. Strip malicious phrases
    String[] verbs = {"ignore", "forget", "override", "reveal", "leak", "print"};
    String[] nouns = {"instructions", "prompt", "context", "previous", "system"};

    for (String verb : verbs) {
        for (String noun : nouns) {
            // Recreate the target phrase
            String badPhrase = verb + " " + noun;
            
            // Keep replacing until it's completely gone from the string
            while (sanitized.contains(badPhrase)) {
                sanitized = sanitized.replace(badPhrase, " ");
            }
        }
    }

    // 2. Strip repeating delimiters (3 or more)
    String[] delimiters = {"---", "===", "~~~", "###", "***"};
    for (String delimiter : delimiters) {
        while (sanitized.contains(delimiter)) {
            sanitized = sanitized.replace(delimiter, " ");
        }
    }

    // 3. Clean up any weird double spaces we accidentally
    //  created
    while (sanitized.contains("  ")) {
        sanitized = sanitized.replace("  ", " ");
    }

    return sanitized.trim();
}
}

