package org.example.arp_ecurity_layer.contracts;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Inbound Kafka event from banking monolith transaction.completed topic.
 * Contains PII — NEVER forward this payload to Spring AI service.
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionCompletedInboundEvent {

    private String eventId;
    private Long transactionId;
    private String transactionReference;
    private String transactionType;
    private Long accountId;
    private String accountNumber;       // PII
    private Long customerId;            // PII
    private String userId;              // PII
    private String email;               // PII
    private BigDecimal amount;
    private BigDecimal balanceAfterTransaction;
    private String remarks;             // May contain PII — must scrub
    private String outflowChannel;
    private Boolean selfTransfer;
    private LocalDateTime occurredAt;
}


