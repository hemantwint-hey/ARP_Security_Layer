package org.example.arp_ecurity_layer.guard.service;

import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.arp_ecurity_layer.contracts.AiSavingGuardPolicyEvent;
import org.example.arp_ecurity_layer.guard.dto.RedisGuardPolicyPayload;
import org.example.arp_ecurity_layer.guard.entity.CustomerSavingPreference;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

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
        // TODO: Parse pref.blockedCategoriesJson (JSON array String) into List<String>
        // TODO: Union user's blocked categories with aiPolicy.recommendedBlockedCategories
        // TODO: Compute remainingMonthlyBudget = pref.monthlyDiscretionaryCap - aiPolicy.currentDiscretionaryTotal
        //       (handle null cap -> remainingMonthlyBudget = null)
        // TODO: Build RedisGuardPolicyPayload with merged data and lastUpdated = now ISO-8601
        // TODO: Serialize to JSON using objectMapper
        // TODO: SET guard:policy:{customerId} = json with TTL = GUARD_TTL
        // TODO: Log: "Synced guard policy for customer {} - guardLevel={}, blockedCategories={}"
        throw new UnsupportedOperationException("TODO: implement");
    }

    /**
     * Writes customer's manual preferences to Redis immediately (without AI policy merge).
     * Called when customer updates preferences via PUT /api/v1/saving-mode/preferences.
     *
     * @param customerId real customer ID
     * @param pref       freshly saved preference entity
     */
    public void syncPreferenceOnlyToRedis(Long customerId, CustomerSavingPreference pref) {
        // TODO: Parse pref.blockedCategoriesJson into List<String>
        // TODO: Build RedisGuardPolicyPayload with pref fields (categorySpendTotals = empty map)
        // TODO: Serialize and SET guard:policy:{customerId} with GUARD_TTL
        throw new UnsupportedOperationException("TODO: implement");
    }

    /**
     * Evicts guard policy from Redis.
     * Called on identity revocation or when customer disables saving mode (guardLevel=OFF).
     *
     * @param customerId real customer ID
     */
    public void evictCustomerPolicy(Long customerId) {
        // TODO: redisTemplate.delete(GUARD_KEY_PREFIX + customerId)
        // TODO: Log: "Evicted guard policy for customer {}"
        throw new UnsupportedOperationException("TODO: implement");
    }
}


