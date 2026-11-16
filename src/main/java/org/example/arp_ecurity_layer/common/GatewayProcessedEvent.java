package org.example.arp_ecurity_layer.common;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Idempotency guard entity for Kafka consumers in the ARP Gateway.
 * Prevents duplicate processing when Kafka redelivers messages.
 * Pattern: check existsByEventId() BEFORE processing, persist AFTER (inside same @Transactional).
 */
@Entity
@Table(name = "gateway_processed_events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GatewayProcessedEvent {

    @Id
    @Column(name = "event_id", length = 64)
    private String eventId;

    @Column(name = "topic_name", nullable = false, length = 80)
    private String topicName;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;
}

