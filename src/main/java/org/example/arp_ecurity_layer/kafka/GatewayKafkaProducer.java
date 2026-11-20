package org.example.arp_ecurity_layer.kafka;

import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.arp_ecurity_layer.contracts.ArpIdentityRevokedEvent;
import org.example.arp_ecurity_layer.contracts.KafkaTopics;
import org.example.arp_ecurity_layer.contracts.SanitizedTransactionEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class GatewayKafkaProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Publishes zero-PII sanitized transaction event to Spring AI service.
     * Partition key = arpToken (guarantees ordering per customer token).
     *
     * @param event zero-PII sanitized event (no customerId, userId, email, accountNumber)
     */
    public void publishSanitizedTransaction(SanitizedTransactionEvent event) {
        // TODO: Serialize event to JSON string using objectMapper
        // TODO: kafkaTemplate.send(KafkaTopics.ARP_SANITIZED_TRANSACTION, event.getArpToken(), json)
        // TODO: Log: "Published sanitized event {} for arpToken {}"
        // TODO: Catch JsonProcessingException and wrap in RuntimeException
        try{
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(KafkaTopics.ARP_SANITIZED_TRANSACTION,event.getArpToken(),json);
            log.info("Published sanitized transaction event {} for token {}",event.getEventId(),event);
        }
        catch(Exception e){
            log.error("Failed to serialize and publisj sanitized error");
            throw new RuntimeException("Kafka publish  failed", e);
        }

    }

    /**
     * Publishes identity revocation tombstone — triggers Right-to-Erasure in Spring AI service.
     * Spring AI will delete all anon_spend_features, anon_recurring_patterns, anon_ai_insights
     * WHERE arp_token = revokedToken.
     *
     * @param event revocation event with arpToken and reason
     */
    public void publishIdentityRevoked(ArpIdentityRevokedEvent event) {
        // TODO: Serialize event to JSON string
        // TODO: kafkaTemplate.send(KafkaTopics.ARP_IDENTITY_REVOKED, event.getArpToken(), json)
        // TODO: Log: "Published identity revocation tombstone for arpToken {}"
        try{
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(KafkaTopics.ARP_IDENTITY_REVOKED, event.getArpToken(),json);
            log.warn("Published identity revocation tombstone for token {}", event.getArpToken());
        }
        catch (Exception obj){
            log.error("Failed to publish identity revocation event", obj);
            throw new RuntimeException("Kafka published failed", obj);
        }

    }
}


