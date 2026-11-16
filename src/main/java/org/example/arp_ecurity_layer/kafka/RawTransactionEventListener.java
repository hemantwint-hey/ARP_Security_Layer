package org.example.arp_ecurity_layer.kafka;

import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.arp_ecurity_layer.common.GatewayProcessedEvent;
import org.example.arp_ecurity_layer.common.GatewayProcessedEventRepository;
import org.example.arp_ecurity_layer.contracts.KafkaTopics;
import org.example.arp_ecurity_layer.contracts.SanitizedTransactionEvent;
import org.example.arp_ecurity_layer.contracts.TransactionCompletedInboundEvent;
import org.example.arp_ecurity_layer.crypto.HmacClusterHasher;
import org.example.arp_ecurity_layer.privacy.service.ArpTokenResolverService;
import org.example.arp_ecurity_layer.privacy.service.DataMinimizer;
import org.example.arp_ecurity_layer.privacy.service.PiiScrubber;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class RawTransactionEventListener {

    private final ArpTokenResolverService tokenResolver;
    private final PiiScrubber piiScrubber;
    private final DataMinimizer dataMinimizer;
    private final GatewayKafkaProducer producer;
    private final GatewayProcessedEventRepository processedEventRepo;
    private final HmacClusterHasher hmacClusterHasher;
    private final ObjectMapper objectMapper;

    @RetryableTopic(
            attempts = "4",
            backOff = @BackOff(delay = 1000, multiplier = 2.0, maxDelay = 10000),
            autoCreateTopics = "true",
            exclude = {IllegalArgumentException.class, NullPointerException.class}
    )
    @KafkaListener(topics = KafkaTopics.TRANSACTION_COMPLETED, groupId = "${arp.kafka.gateway-group}")
    @Transactional
    public void onTransactionCompleted(String payload) {
        // TODO: 1. Deserialize payload -> TransactionCompletedInboundEvent using objectMapper
        // TODO: 2. Idempotency check: if processedEventRepo.existsByEventId(event.getEventId()) -> log & return
        // TODO: 3. Resolve arpToken: tokenResolver.getOrCreateActiveToken(customerId, userId)
        // TODO: 4. If arpToken is empty (consent disabled) -> log "Skipping: consent disabled for customer {}" & return
        // TODO: 5. Scrub PII from remarks: piiScrubber.scrubFreeText(event.getRemarks())
        // TODO: 6. Get timeBucket: dataMinimizer.toTimeBucket(event.getOccurredAt())
        // TODO: 7. Get amountBand: dataMinimizer.toAmountBand(event.getAmount())
        // TODO: 8. Get minimizedAmount: dataMinimizer.toMinimizedAmount(event.getAmount(), privacyMode)
        // TODO: 9. Get periodKey: dataMinimizer.toPeriodKey(event.getOccurredAt())
        // TODO: 10. Get merchantClusterHash: hmacClusterHasher.hash(scrubResult.scrubbedText())
        // TODO: 11. Compute sourceEventHash: SHA-256 of event.getEventId() (non-reversible)
        // TODO: 12. Build SanitizedTransactionEvent (zero PII - only arpToken, bands, buckets)
        // TODO: 13. Publish: producer.publishSanitizedTransaction(sanitizedEvent)
        // TODO: 14. Persist idempotency: GatewayProcessedEvent(eventId, topic, now)
        // TODO: 15. Persist PrivacyAuditLog (customerId, arpToken, action=SANITIZED_FORWARD, fieldsStripped)
        // TODO: 16. Log: "Sanitized and forwarded transaction {} for arpToken {}"
        throw new UnsupportedOperationException("TODO: implement");
    }
}

