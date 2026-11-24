package org.example.arp_ecurity_layer;

import tools.jackson.databind.ObjectMapper;
import org.example.arp_ecurity_layer.contracts.AiSavingGuardPolicyEvent;
import org.example.arp_ecurity_layer.guard.entity.CustomerSavingPreference;
import org.example.arp_ecurity_layer.guard.service.SavingGuardSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SavingGuardSyncServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private SavingGuardSyncService syncService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        syncService = new SavingGuardSyncService(redisTemplate, objectMapper);
    }

    @Test
    @DisplayName("Merges user preferences with AI policy recommendations and writes to Redis")
    void shouldMergeAndSyncPolicyToRedis() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        Long customerId = 123L;
        CustomerSavingPreference pref = CustomerSavingPreference.builder()
                .customerId(customerId)
                .guardLevel("STRICT_LOCK")
                .monthlyDiscretionaryCap(new BigDecimal("15000.00"))
                .singleTxSoftCap(new BigDecimal("2000.00"))
                .blockedCategoriesJson("[\"SHOPPING\"]")
                .failClosedOnOutage(false)
                .updatedAt(LocalDateTime.now())
                .build();

        AiSavingGuardPolicyEvent aiPolicy = new AiSavingGuardPolicyEvent();
        aiPolicy.setEventId("pol-1");
        aiPolicy.setArpToken("R789-9a4f");
        aiPolicy.setRecommendedBlockedCategories(List.of("FOOD_DELIVERY", "SUBSCRIPTIONS"));
        aiPolicy.setCurrentDiscretionaryTotal(new BigDecimal("13500.00"));
        aiPolicy.setCategorySpendTotals(Map.of("FOOD_DELIVERY", new BigDecimal("5800.00")));

        syncService.syncCustomerPolicyToRedis(customerId, pref, aiPolicy);

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq("guard:policy:123"), jsonCaptor.capture(), any(Duration.class));

        String savedJson = jsonCaptor.getValue();
        assertNotNull(savedJson);
        assertTrue(savedJson.contains("STRICT_LOCK"));
        assertTrue(savedJson.contains("SHOPPING"));
        assertTrue(savedJson.contains("FOOD_DELIVERY"));
        assertTrue(savedJson.contains("SUBSCRIPTIONS"));
        assertTrue(savedJson.contains("1500.00")); // remaining budget: 15000 - 13500
    }

    @Test
    @DisplayName("Evicts guard policy from Redis on revocation")
    void shouldEvictPolicy() {
        syncService.evictCustomerPolicy(456L);
        verify(redisTemplate).delete("guard:policy:456");
    }
}
