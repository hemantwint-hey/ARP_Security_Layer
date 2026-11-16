package org.example.arp_ecurity_layer.contracts;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Zero-PII Kafka event published to arp.sanitized.transaction topic.
 * This event MUST contain no PII: no customerId, userId, email, accountNumber, or exact timestamps.
 */
@Data
@Builder
public class SanitizedTransactionEvent {

    private String eventId;
    private String sourceEventHash;         // SHA-256 of original eventId (non-reversible)
    private String arpToken;               // Ephemeral token e.g. R789-9a4f
    private String privacyMode;            // BALANCED | STRICT
    private String channel;               // TRANSFER | WITHDRAWAL
    private String amountBand;            // MICRO | LOW | MEDIUM | HIGH | VERY_HIGH
    private BigDecimal minimizedAmount;   // null in STRICT mode
    private String timeBucket;            // e.g. WEEKDAY_NIGHT
    private String periodKey;             // e.g. 2026-11
    private String sanitizedDescriptor;   // scrubbed & truncated remarks
    private String merchantClusterHash;   // HMAC of normalized descriptor
}
