package org.example.arp_ecurity_layer.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.arp_ecurity_layer.common.GatewayProcessedEvent;
import org.example.arp_ecurity_layer.common.GatewayProcessedEventRepository;
import org.example.arp_ecurity_layer.contracts.KafkaTopics;
import org.example.arp_ecurity_layer.contracts.SanitizedTransactionEvent;
import org.example.arp_ecurity_layer.contracts.TransactionCompletedInboundEvent;
import org.example.arp_ecurity_layer.crypto.HmacClusterHasher;
import org.example.arp_ecurity_layer.privacy.entity.PrivacyAuditLog;
import org.example.arp_ecurity_layer.privacy.repository.PrivacyAuditLogRepository;
import org.example.arp_ecurity_layer.privacy.service.ArpTokenResolverService;
import org.example.arp_ecurity_layer.privacy.service.DataMinimizer;
import org.example.arp_ecurity_layer.privacy.service.PiiScrubber;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class RawTransactionEventListener {

    private final ArpTokenResolverService tokenResolver;
    private final PiiScrubber piiScrubber;
    private final DataMinimizer dataMinimizer;
    private final GatewayKafkaProducer producer;
    private final GatewayProcessedEventRepository processedEventRepo;
    private final PrivacyAuditLogRepository auditLogRepo;
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
        try {
            // 1. Deserialize
            TransactionCompletedInboundEvent event = objectMapper.readValue(payload, TransactionCompletedInboundEvent.class);

            // 2. Idempotency Check
            if (processedEventRepo.existsByEventId(event.getEventId())) {
                log.info("Event {} already processed, skipping duplicate", event.getEventId());
                return;
            }

            // 3. Resolve / Mint ARP Token
            Optional<String> arpTokenOpt = tokenResolver.getOrCreateActiveToken(event.getCustomerId(), event.getUserId());
            if (arpTokenOpt.isEmpty()) {
                log.info("Customer {} opted out of AI analysis, skipping event", event.getCustomerId());
                markProcessed(event.getEventId(), KafkaTopics.TRANSACTION_COMPLETED);
                return;
            }
            String arpToken = arpTokenOpt.get();

            // 4. Scrub Free-Text Remarks
            PiiScrubber.ScrubResult scrubResult = piiScrubber.scrubFreeText(event.getRemarks());

            // 5. Coarsen & Minimize Data
            String timeBucket = dataMinimizer.toTimeBucket(event.getOccurredAt());
            String amountBand = dataMinimizer.toAmountBand(event.getAmount());
            var minimizedAmount = dataMinimizer.toMinimizedAmount(event.getAmount(), "BALANCED");
            String periodKey = dataMinimizer.toPeriodKey(event.getOccurredAt());
            String clusterHash = hmacClusterHasher.hash(scrubResult.scrubbedText());

            // 6. Assemble Zero-PII Sanitized Event
            SanitizedTransactionEvent sanitizedEvent = SanitizedTransactionEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .sourceEventHash(sha256(event.getEventId()))
                    .arpToken(arpToken)
                    .privacyMode("BALANCED")
                    .channel(event.getTransactionType())
                    .amountBand(amountBand)
                    .minimizedAmount(minimizedAmount)
                    .timeBucket(timeBucket)
                    .periodKey(periodKey)
                    .sanitizedDescriptor(scrubResult.scrubbedText())
                    .merchantClusterHash(clusterHash)
                    .build();

            // 7. Publish to Kafka
            producer.publishSanitizedTransaction(sanitizedEvent);

            // 8. Record Idempotency & Audit Trail
            markProcessed(event.getEventId(), KafkaTopics.TRANSACTION_COMPLETED);
            auditLogRepo.save(PrivacyAuditLog.builder()
                    .customerId(event.getCustomerId())
                    .arpToken(arpToken)
                    .auditAction("SANITIZED_FORWARD")
                    .fieldsStripped(scrubResult.fieldsStripped().isBlank() ? "IDENTITY_MASKED" : scrubResult.fieldsStripped())
                    .payloadSha256(sanitizedEvent.getSourceEventHash())
                    .createdAt(LocalDateTime.now())
                    .build());

            log.info("Successfully sanitized & forwarded transaction {} for token {}", event.getEventId(), arpToken);
        } catch (Exception e) {
            log.error("Failed processing transaction.completed event", e);
            throw new RuntimeException("Transaction pipeline error", e);
        }
    }

    private void markProcessed(String eventId, String topic) {
        processedEventRepo.save(GatewayProcessedEvent.builder()
                .eventId(eventId)
                .topicName(topic)
                .processedAt(LocalDateTime.now())
                .build());
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (Exception e) {
            return "HASH_ERROR";
        }
    }
}
