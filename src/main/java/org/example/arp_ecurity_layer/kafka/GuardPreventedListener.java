package org.example.arp_ecurity_layer.kafka;

import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.arp_ecurity_layer.common.GatewayProcessedEvent;
import org.example.arp_ecurity_layer.common.GatewayProcessedEventRepository;
import org.example.arp_ecurity_layer.contracts.GuardPreventedEvent;
import org.example.arp_ecurity_layer.contracts.KafkaTopics;
import org.example.arp_ecurity_layer.guard.entity.SavingGuardMetric;
import org.example.arp_ecurity_layer.guard.repository.SavingGuardMetricRepository;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class GuardPreventedListener {

    private final GatewayProcessedEventRepository processedEventRepo;
    private final SavingGuardMetricRepository savingGuardMetricRepo;
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
        try {
            // 1. Deserialize payload -> GuardPreventedEvent
            GuardPreventedEvent event = objectMapper.readValue(payload, GuardPreventedEvent.class);

            // 2. Idempotency check
            if (processedEventRepo.existsByEventId(event.getEventId())) {
                log.info("Guard prevented event {} already processed, skipping duplicate", event.getEventId());
                return;
            }

            // 3. Build SavingGuardMetric from event
            SavingGuardMetric metric = SavingGuardMetric.builder()
                    .customerId(event.getCustomerId())
                    .transactionType(event.getTransactionType() != null ? event.getTransactionType() : "UNKNOWN")
                    .category(event.getCategory() != null ? event.getCategory() : "UNCATEGORIZED")
                    .savedAmount(event.getPreventedAmount())
                    .actionTaken(event.getActionTaken() != null ? event.getActionTaken() : "BLOCKED")
                    .createdAt(event.getOccurredAt() != null ? event.getOccurredAt() : LocalDateTime.now())
                    .build();

            // 4. Save metric
            savingGuardMetricRepo.save(metric);

            // 5. Persist idempotency record
            processedEventRepo.save(new GatewayProcessedEvent(event.getEventId(), KafkaTopics.GUARD_PREVENTED, LocalDateTime.now()));

            log.info("Guard saved {} INR for customer {} in category {}",
                    event.getPreventedAmount(), event.getCustomerId(), event.getCategory());

        } catch (Exception e) {
            log.error("Error processing arp.guard.prevented event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process arp.guard.prevented event", e);
        }
    }
}
