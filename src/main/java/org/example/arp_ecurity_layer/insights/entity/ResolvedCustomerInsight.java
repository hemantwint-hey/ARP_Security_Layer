package org.example.arp_ecurity_layer.insights.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "resolved_customer_insights", indexes = {
        @Index(name = "idx_insight_cust_created", columnList = "customer_id, created_at DESC")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ResolvedCustomerInsight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "source_event_id", nullable = false, unique = true, length = 64)
    private String sourceEventId;

    /** SUBSCRIPTION_LEAK | SPEND_SPIKE | SAVINGS_TIP */
    @Column(name = "insight_type", nullable = false, length = 40)
    private String insightType;

    /** INFO | WARN | HIGH */
    @Column(name = "severity", nullable = false, length = 16)
    private String severity;

    @Column(name = "category", nullable = false, length = 40)
    private String category;

    @Column(name = "title", nullable = false, length = 160)
    private String title;

    @Column(name = "recommendation", nullable = false, columnDefinition = "TEXT")
    private String recommendation;

    @Column(name = "estimated_monthly_savings", nullable = false, precision = 18, scale = 2)
    private BigDecimal estimatedMonthlySavings;

    @Column(name = "acknowledged", nullable = false)
    private boolean acknowledged;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

