package org.example.arp_ecurity_layer.guard.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "saving_guard_metrics", indexes = {
        @Index(name = "idx_guard_cust_created", columnList = "customer_id, created_at DESC")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SavingGuardMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "transaction_type", nullable = false, length = 30)
    private String transactionType;

    @Column(name = "category", nullable = false, length = 40)
    private String category;

    @Column(name = "saved_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal savedAmount;

    /** BLOCKED | USER_CANCELLED_ON_WARN */
    @Column(name = "action_taken", nullable = false, length = 30)
    private String actionTaken;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

