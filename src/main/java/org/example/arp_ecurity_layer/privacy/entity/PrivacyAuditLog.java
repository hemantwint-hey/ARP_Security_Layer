package org.example.arp_ecurity_layer.privacy.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "privacy_audit_logs", indexes = {
        @Index(name = "idx_privacy_audit_cust", columnList = "customer_id, created_at DESC")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PrivacyAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "arp_token", length = 64)
    private String arpToken;

    /** TOKEN_MINTED | SANITIZED_FORWARD | ROTATED | REVOKED */
    @Column(name = "audit_action", nullable = false, length = 40)
    private String auditAction;

    @Column(name = "fields_stripped", nullable = false, length = 255)
    private String fieldsStripped;

    @Column(name = "payload_sha256", length = 64)
    private String payloadSha256;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

