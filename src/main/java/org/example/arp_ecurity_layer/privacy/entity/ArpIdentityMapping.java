package org.example.arp_ecurity_layer.privacy.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "arp_identity_mappings", indexes = {
        @Index(name = "idx_arp_customer_status", columnList = "customer_id, status"),
        @Index(name = "idx_arp_expires_at",       columnList = "expires_at")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ArpIdentityMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "arp_token", nullable = false, unique = true, length = 64)
    private String arpToken;

    /** STRICT | BALANCED */
    @Column(name = "privacy_mode", nullable = false, length = 20)
    private String privacyMode;

    @Column(name = "consent_enabled", nullable = false)
    private boolean consentEnabled;

    @Column(name = "salt_version", nullable = false)
    private int saltVersion;

    /** ACTIVE | ROTATED | REVOKED */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

