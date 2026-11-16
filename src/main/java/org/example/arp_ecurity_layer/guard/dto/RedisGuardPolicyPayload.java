package org.example.arp_ecurity_layer.guard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Redis-cached guard policy stored at key: guard:policy:{customerId}
 * Serialized as JSON. Read by banking monolith SavingGuardEvaluator in < 2ms.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedisGuardPolicyPayload {

    /** OFF | SOFT_GUARD | STRICT_LOCK */
    private String guardLevel;

    private List<String> blockedCategories;
    private BigDecimal monthlyDiscretionaryCap;
    private BigDecimal singleTxSoftCap;

    /** AI-computed spend totals per category for current period */
    private Map<String, BigDecimal> categorySpendTotals;

    private BigDecimal remainingMonthlyBudget;
    private boolean failClosedOnOutage;
    private String lastUpdated;              // ISO-8601 timestamp string
}

