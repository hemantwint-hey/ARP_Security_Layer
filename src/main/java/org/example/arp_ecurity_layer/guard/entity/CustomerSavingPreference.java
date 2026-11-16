package org.example.arp_ecurity_layer.guard.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_saving_preferences")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CustomerSavingPreference {

    @Id
    @Column(name = "customer_id")
    private Long customerId;

    /** OFF | SOFT_GUARD | STRICT_LOCK */
    @Column(name = "guard_level", nullable = false, length = 20)
    private String guardLevel;

    @Column(name = "monthly_discretionary_cap", precision = 18, scale = 2)
    private BigDecimal monthlyDiscretionaryCap;

    @Column(name = "single_tx_soft_cap", precision = 18, scale = 2)
    private BigDecimal singleTxSoftCap;

    /** JSON array of blocked category strings e.g. ["FOOD_DELIVERY","SHOPPING"] */
    @Column(name = "blocked_categories_json", columnDefinition = "JSON", nullable = false)
    private String blockedCategoriesJson;

    @Column(name = "fail_closed_on_outage", nullable = false)
    private boolean failClosedOnOutage;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

