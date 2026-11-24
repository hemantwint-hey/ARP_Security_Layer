package org.example.arp_ecurity_layer.guard.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.arp_ecurity_layer.contracts.AiSavingGuardPolicyEvent;
import org.example.arp_ecurity_layer.guard.dto.RedisGuardPolicyPayload;
import org.example.arp_ecurity_layer.guard.entity.CustomerSavingPreference;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SavingGuardSyncService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String GUARD_KEY_PREFIX = "guard:policy:";
    private static final Duration GUARD_TTL      = Duration.ofHours(25);  // Slightly over 1 day

    /**
     * Merges user's DB preferences with AI-computed policy and writes final JSON to Redis.
     * Called after AiSavingGuardPolicyEvent is received and de-tokenized by AiResultDeTokenizerListener.
     * Banking monolith reads this Redis key (< 2ms) before every debit to enforce guard rules.
     *
     * @param customerId real customer ID (resolved from arpToken)
     * @param pref       customer's saved preferences from DB
     * @param aiPolicy   AI-computed spend policy event (zero-PII, uses arpToken internally)
     */
    public void syncCustomerPolicyToRedis(Long customerId,
                                          CustomerSavingPreference pref,
                                          AiSavingGuardPolicyEvent aiPolicy) {
        try {
            // 1. Parse user's manual blocked categories
            Set<String> mergedCategories = new LinkedHashSet<>(parseCategories(pref.getBlockedCategoriesJson()));

            // 2. Union with AI recommended blocked categories
            if (aiPolicy != null && aiPolicy.getRecommendedBlockedCategories() != null) {
                mergedCategories.addAll(aiPolicy.getRecommendedBlockedCategories());
            }

            // 3. Compute remainingMonthlyBudget = monthlyDiscretionaryCap - currentDiscretionaryTotal
            BigDecimal remainingMonthlyBudget = null;
            if (pref.getMonthlyDiscretionaryCap() != null) {
                if (aiPolicy != null && aiPolicy.getCurrentDiscretionaryTotal() != null) {
                    remainingMonthlyBudget = pref.getMonthlyDiscretionaryCap().subtract(aiPolicy.getCurrentDiscretionaryTotal());
                } else {
                    remainingMonthlyBudget = pref.getMonthlyDiscretionaryCap();
                }
            }

            Map<String, BigDecimal> spendTotals = (aiPolicy != null && aiPolicy.getCategorySpendTotals() != null)
                    ? aiPolicy.getCategorySpendTotals()
                    : Collections.emptyMap();

            // 4. Build RedisGuardPolicyPayload
            RedisGuardPolicyPayload payload = RedisGuardPolicyPayload.builder()
                    .guardLevel(pref.getGuardLevel() != null ? pref.getGuardLevel() : "OFF")
                    .blockedCategories(new ArrayList<>(mergedCategories))
                    .monthlyDiscretionaryCap(pref.getMonthlyDiscretionaryCap())
                    .singleTxSoftCap(pref.getSingleTxSoftCap())
                    .categorySpendTotals(spendTotals)
                    .remainingMonthlyBudget(remainingMonthlyBudget)
                    .failClosedOnOutage(pref.isFailClosedOnOutage())
                    .lastUpdated(Instant.now().toString())
                    .build();

            // 5. Serialize to JSON and write to Redis
            String json = objectMapper.writeValueAsString(payload);
            redisTemplate.opsForValue().set(GUARD_KEY_PREFIX + customerId, json, GUARD_TTL);

            log.info("Synced guard policy for customer {} - guardLevel={}, blockedCategories={}",
                    customerId, payload.getGuardLevel(), payload.getBlockedCategories());

        } catch (Exception e) {
            log.error("Failed to sync guard policy to Redis for customer {}: {}", customerId, e.getMessage(), e);
        }
    }

    /**
     * Writes customer's manual preferences to Redis immediately (without AI policy merge).
     * Called when customer updates preferences via PUT /api/v1/saving-mode/preferences.
     *
     * @param customerId real customer ID
     * @param pref       freshly saved preference entity
     */
    public void syncPreferenceOnlyToRedis(Long customerId, CustomerSavingPreference pref) {
        try {
            List<String> blockedCategories = parseCategories(pref.getBlockedCategoriesJson());

            RedisGuardPolicyPayload payload = RedisGuardPolicyPayload.builder()
                    .guardLevel(pref.getGuardLevel() != null ? pref.getGuardLevel() : "OFF")
                    .blockedCategories(blockedCategories)
                    .monthlyDiscretionaryCap(pref.getMonthlyDiscretionaryCap())
                    .singleTxSoftCap(pref.getSingleTxSoftCap())
                    .categorySpendTotals(Collections.emptyMap())
                    .remainingMonthlyBudget(pref.getMonthlyDiscretionaryCap())
                    .failClosedOnOutage(pref.isFailClosedOnOutage())
                    .lastUpdated(Instant.now().toString())
                    .build();

            String json = objectMapper.writeValueAsString(payload);
            redisTemplate.opsForValue().set(GUARD_KEY_PREFIX + customerId, json, GUARD_TTL);

            log.info("Synced manual preference for customer {} to Redis", customerId);
        } catch (Exception e) {
            log.error("Failed to sync preference only to Redis for customer {}: {}", customerId, e.getMessage(), e);
        }
    }

    /**
     * Evicts guard policy from Redis.
     * Called on identity revocation or when customer disables saving mode (guardLevel=OFF).
     *
     * @param customerId real customer ID
     */
    public void evictCustomerPolicy(Long customerId) {
        redisTemplate.delete(GUARD_KEY_PREFIX + customerId);
        log.info("Evicted guard policy for customer {}", customerId);
    }

    private List<String> parseCategories(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            List<String> result = new ArrayList<>();
            if (node.isArray()) {
                for (JsonNode item : node) {
                    result.add(item.asText());
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to parse blocked categories JSON '{}': {}", json, e.getMessage());
            return new ArrayList<>();
        }
    }
}
