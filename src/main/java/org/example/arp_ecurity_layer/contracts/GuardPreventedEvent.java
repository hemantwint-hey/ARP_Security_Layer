package org.example.arp_ecurity_layer.contracts;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Inbound Kafka event from banking monolith on arp.guard.prevented topic.
 * Records money saved when AI Guard blocks or cancels a wasteful transaction.
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GuardPreventedEvent {

    private String eventId;
    private Long customerId;
    private String transactionType;
    private String category;
    private BigDecimal preventedAmount;
    private String actionTaken;          // BLOCKED | USER_CANCELLED_ON_WARN
    private LocalDateTime occurredAt;
}



