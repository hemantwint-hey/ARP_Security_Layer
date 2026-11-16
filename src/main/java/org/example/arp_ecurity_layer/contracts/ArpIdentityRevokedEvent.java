package org.example.arp_ecurity_layer.contracts;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Outbound Kafka tombstone event published to arp.identity.revoked.
 * Triggers Spring AI service to delete all data for this arpToken (Right-to-Erasure / GDPR).
 */
@Data
@Builder
public class ArpIdentityRevokedEvent {

    private String eventId;
    private String arpToken;
    private String reason;           // USER_REVOKED_CONSENT | ADMIN_ERASURE | TOKEN_ROTATION
    private LocalDateTime revokedAt;
}

