package org.example.arp_ecurity_layer.contracts;

/**
 * Central registry of all Kafka topic names shared across the ARP platform.
 */
public final class KafkaTopics {

    private KafkaTopics() {}

    // Inbound from Banking Monolith
    public static final String TRANSACTION_COMPLETED    = "transaction.completed";
    public static final String GUARD_PREVENTED          = "arp.guard.prevented";

    // Inbound from Spring AI Advisor Service
    public static final String AI_INSIGHTS_READY        = "ai.insights.ready";
    public static final String AI_SAVING_GUARD_POLICY   = "ai.saving.guard.policy";

    // Outbound from ARP Gateway
    public static final String ARP_SANITIZED_TRANSACTION = "arp.sanitized.transaction";
    public static final String ARP_IDENTITY_REVOKED      = "arp.identity.revoked";
}
