package org.example.arp_ecurity_layer.contracts;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Inbound Kafka event from spring-ai-advisor-service on ai.saving.guard.policy topic.
 * Used to sync AI-computed spend policies into Redis guard:policy:{customerId}.
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiSavingGuardPolicyEvent {

    private String eventId;
    private String arpToken;
    private String periodKey;
    private Map<String, BigDecimal> categorySpendTotals;
    private List<String> recommendedBlockedCategories;
    private BigDecimal currentDiscretionaryTotal;
    private String rationale;
    private String generatedAtPeriod;
}



