package org.example.arp_ecurity_layer.kafka;

import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.arp_ecurity_layer.common.GatewayProcessedEventRepository;
import org.example.arp_ecurity_layer.contracts.GuardPreventedEvent;
import org.example.arp_ecurity_layer.contracts.KafkaTopics;
import org.example.arp_ecurity_layer.guard.entity.SavingGuardMetric;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class GuardPreventedListener {

    private final GatewayProcessedEventRepository processedEventRepo;
    private final ObjectMapper objectMapper;

    @RetryableTopic(
            attempts = "4",
            backOff = @BackOff(delay = 1000, multiplier = 2.0, maxDelay = 10000),
            autoCreateTopics = "true",
            exclude = {IllegalArgumentException.class, NullPointerException.class}
    )
    @KafkaListener(topics = KafkaTopics.GUARD_PREVENTED, groupId = "${arp.kafka.metric-group}")
    @Transactional
    public void onGuardPrevented(String payload) {
        // TODO: 1. Deserialize payload -> GuardPreventedEvent
        // TODO: 2. Idempotency check: processedEventRepo.existsByEventId(event.getEventId())
        // TODO: 3. Build SavingGuardMetric from event (customerId, transactionType, category, savedAmount, actionTaken)
        // TODO: 4. Inject SavingGuardMetricRepository and save metric
        // TODO: 5. Persist idempotency record
        // TODO: 6. Log: "Guard saved {} INR for customer {} in category {}"
        throw new UnsupportedOperationException("TODO: implement");
    }
}

