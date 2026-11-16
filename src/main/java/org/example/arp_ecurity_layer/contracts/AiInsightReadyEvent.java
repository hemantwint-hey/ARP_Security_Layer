package org.example.arp_ecurity_layer.contracts;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Inbound Kafka event from spring-ai-advisor-service on ai.insights.ready topic.
 * Uses arpToken only — no PII.
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiInsightReadyEvent {

    private String eventId;
    private String sourceSanitizedEventId;
    private String arpToken;
    private String insightType;             // SUBSCRIPTION_LEAK | SPEND_SPIKE | SAVINGS_TIP
    private String severity;               // INFO | WARN | HIGH
    private String category;
    private String title;
    private String recommendation;
    private BigDecimal estimatedMonthlySavings;
    private String occurredPeriod;
}


