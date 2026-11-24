package org.example.arp_ecurity_layer.kafka;

import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.arp_ecurity_layer.common.GatewayProcessedEvent;
import org.example.arp_ecurity_layer.common.GatewayProcessedEventRepository;
import org.example.arp_ecurity_layer.contracts.AiInsightReadyEvent;
import org.example.arp_ecurity_layer.contracts.AiSavingGuardPolicyEvent;
import org.example.arp_ecurity_layer.contracts.KafkaTopics;
import org.example.arp_ecurity_layer.guard.entity.CustomerSavingPreference;
import org.example.arp_ecurity_layer.guard.repository.CustomerSavingPreferenceRepository;
import org.example.arp_ecurity_layer.guard.service.SavingGuardSyncService;
import org.example.arp_ecurity_layer.insights.entity.ResolvedCustomerInsight;
import org.example.arp_ecurity_layer.insights.repository.ResolvedCustomerInsightRepository;
import org.example.arp_ecurity_layer.privacy.service.ArpTokenResolverService;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiResultDeTokenizerListener {

    private final ArpTokenResolverService tokenResolver;
    private final ResolvedCustomerInsightRepository insightRepo;
    private final CustomerSavingPreferenceRepository prefRepo;
    private final SavingGuardSyncService guardSyncService;
    private final GatewayProcessedEventRepository processedEventRepo;
    private final ObjectMapper objectMapper;

    @RetryableTopic(
            attempts = "4",
            backOff = @BackOff(delay = 1000, multiplier = 2.0, maxDelay = 10000),
            autoCreateTopics = "true",
            exclude = {IllegalArgumentException.class, NullPointerException.class}
    )
    @KafkaListener(topics = KafkaTopics.AI_INSIGHTS_READY, groupId = "${arp.kafka.insight-resolver-group}")
    @Transactional
    public void onInsightReady(String payload) {
        try {
            // 1. Deserialize payload -> AiInsightReadyEvent
            AiInsightReadyEvent event = objectMapper.readValue(payload, AiInsightReadyEvent.class);

            // 2. Idempotency check
            if (processedEventRepo.existsByEventId(event.getEventId())) {
                log.info("Insight event {} already processed, skipping duplicate", event.getEventId());
                return;
            }

            // 3. Resolve customerId from arpToken
            Optional<Long> customerIdOpt = tokenResolver.resolveCustomerId(event.getArpToken());
            if (customerIdOpt.isEmpty()) {
                log.warn("No active customer found for arpToken {}, skipping insight {}", event.getArpToken(), event.getEventId());
                processedEventRepo.save(new GatewayProcessedEvent(event.getEventId(), KafkaTopics.AI_INSIGHTS_READY, LocalDateTime.now()));
                return;
            }

            Long customerId = customerIdOpt.get();

            // 4. Build ResolvedCustomerInsight from event fields
            ResolvedCustomerInsight insight = ResolvedCustomerInsight.builder()
                    .customerId(customerId)
                    .sourceEventId(event.getEventId())
                    .insightType(event.getInsightType())
                    .severity(event.getSeverity())
                    .category(event.getCategory())
                    .title(event.getTitle())
                    .recommendation(event.getRecommendation())
                    .estimatedMonthlySavings(event.getEstimatedMonthlySavings() != null ? event.getEstimatedMonthlySavings() : BigDecimal.ZERO)
                    .acknowledged(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            insightRepo.save(insight);

            // 5. Save idempotency record
            processedEventRepo.save(new GatewayProcessedEvent(event.getEventId(), KafkaTopics.AI_INSIGHTS_READY, LocalDateTime.now()));

            log.info("Resolved and saved insight {} for customer {}", event.getEventId(), customerId);

        } catch (Exception e) {
            log.error("Error processing ai.insights.ready event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process ai.insights.ready event", e);
        }
    }

    @RetryableTopic(
            attempts = "4",
            backOff = @BackOff(delay = 1000, multiplier = 2.0, maxDelay = 10000),
            autoCreateTopics = "true",
            exclude = {IllegalArgumentException.class, NullPointerException.class}
    )
    @KafkaListener(topics = KafkaTopics.AI_SAVING_GUARD_POLICY, groupId = "${arp.kafka.guard-sync-group}")
    @Transactional
    public void onGuardPolicyReady(String payload) {
        try {
            // 1. Deserialize payload -> AiSavingGuardPolicyEvent
            AiSavingGuardPolicyEvent aiPolicy = objectMapper.readValue(payload, AiSavingGuardPolicyEvent.class);

            // 2. Idempotency check
            if (processedEventRepo.existsByEventId(aiPolicy.getEventId())) {
                log.info("Guard policy event {} already processed, skipping duplicate", aiPolicy.getEventId());
                return;
            }

            // 3. Resolve arpToken -> customerId
            Optional<Long> customerIdOpt = tokenResolver.resolveCustomerId(aiPolicy.getArpToken());
            if (customerIdOpt.isEmpty()) {
                log.warn("No active customer found for arpToken {}, skipping policy sync", aiPolicy.getArpToken());
                processedEventRepo.save(new GatewayProcessedEvent(aiPolicy.getEventId(), KafkaTopics.AI_SAVING_GUARD_POLICY, LocalDateTime.now()));
                return;
            }

            Long customerId = customerIdOpt.get();

            // 4. Load CustomerSavingPreference from prefRepo (use default if absent)
            CustomerSavingPreference pref = prefRepo.findById(customerId).orElseGet(() ->
                    CustomerSavingPreference.builder()
                            .customerId(customerId)
                            .guardLevel("OFF")
                            .blockedCategoriesJson("[]")
                            .failClosedOnOutage(false)
                            .updatedAt(LocalDateTime.now())
                            .build()
            );

            // 5. Sync to Redis
            guardSyncService.syncCustomerPolicyToRedis(customerId, pref, aiPolicy);

            // 6. Persist idempotency record
            processedEventRepo.save(new GatewayProcessedEvent(aiPolicy.getEventId(), KafkaTopics.AI_SAVING_GUARD_POLICY, LocalDateTime.now()));

            log.info("Synced AI guard policy for customer {}", customerId);

        } catch (Exception e) {
            log.error("Error processing ai.saving.guard.policy event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process ai.saving.guard.policy event", e);
        }
    }
}
