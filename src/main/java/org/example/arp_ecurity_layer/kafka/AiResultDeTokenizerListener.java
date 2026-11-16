package org.example.arp_ecurity_layer.kafka;

import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.arp_ecurity_layer.common.GatewayProcessedEventRepository;
import org.example.arp_ecurity_layer.contracts.AiInsightReadyEvent;
import org.example.arp_ecurity_layer.contracts.AiSavingGuardPolicyEvent;
import org.example.arp_ecurity_layer.contracts.KafkaTopics;
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
        // TODO: 1. Deserialize payload -> AiInsightReadyEvent
        // TODO: 2. Idempotency check: if processedEventRepo.existsByEventId(event.getEventId()) -> return
        // TODO: 3. Resolve: tokenResolver.resolveCustomerId(event.getArpToken())
        // TODO: 4. If empty (token rotated/revoked) -> log WARNING "No customer for arpToken, skipping insight" -> return
        // TODO: 5. Build ResolvedCustomerInsight from event fields with real customerId
        // TODO: 6. insightRepo.save(insight)
        // TODO: 7. processedEventRepo.save(GatewayProcessedEvent(eventId, topic, now))
        // TODO: 8. Log: "Resolved and saved insight {} for customer {}"
        throw new UnsupportedOperationException("TODO: implement");
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
        // TODO: 1. Deserialize payload -> AiSavingGuardPolicyEvent
        // TODO: 2. Idempotency check
        // TODO: 3. Resolve arpToken -> customerId
        // TODO: 4. If empty -> log and return
        // TODO: 5. Load CustomerSavingPreference from prefRepo (use defaults if not found)
        // TODO: 6. guardSyncService.syncCustomerPolicyToRedis(customerId, pref, aiPolicy)
        // TODO: 7. Persist idempotency record
        // TODO: 8. Log: "Synced AI guard policy for customer {}"
        throw new UnsupportedOperationException("TODO: implement");
    }
}

