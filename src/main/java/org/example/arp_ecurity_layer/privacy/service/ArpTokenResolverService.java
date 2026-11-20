package org.example.arp_ecurity_layer.privacy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.arp_ecurity_layer.contracts.ArpIdentityRevokedEvent;
import org.example.arp_ecurity_layer.crypto.AesGcmEncryptor;
import org.example.arp_ecurity_layer.kafka.GatewayKafkaProducer;
import org.example.arp_ecurity_layer.privacy.entity.ArpIdentityMapping;
import org.example.arp_ecurity_layer.privacy.entity.PrivacyAuditLog;
import org.example.arp_ecurity_layer.privacy.repository.ArpIdentityMappingRepository;
import org.example.arp_ecurity_layer.privacy.repository.PrivacyAuditLogRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArpTokenResolverService {

    private final ArpIdentityMappingRepository mappingRepo;
    private final PrivacyAuditLogRepository auditLogRepo;
    private final StringRedisTemplate redisTemplate;
    private final AesGcmEncryptor aesEncryptor;
    private final GatewayKafkaProducer producer;

    private static final String ARP_FORWARD_PREFIX = "arp:map:";     // customerId -> arpToken
    private static final String ARP_REVERSE_PREFIX = "arp:resolve:"; // arpToken -> customerId
    private static final Duration CACHE_TTL        = Duration.ofHours(24);
    private static final SecureRandom RANDOM       = new SecureRandom();

    /**
     * Returns existing ACTIVE arpToken for customer, or mints a new one.
     * Caches bidirectionally in Redis (TTL 24h).
     *
     * @param customerId real customer ID (PII - never leaves Trust Zone B)
     * @param userId     UUID of authenticated user
     * @return Optional arpToken; empty if consent is disabled
     */
    @Transactional
    public Optional<String> getOrCreateActiveToken(Long customerId, String userId) {
        String forwardKey = ARP_FORWARD_PREFIX + customerId;
        String cachedToken = redisTemplate.opsForValue().get(forwardKey);
        if (cachedToken != null) {
            return Optional.of(cachedToken);
        }

        Optional<ArpIdentityMapping> existing = mappingRepo.findByCustomerIdAndStatus(customerId, "ACTIVE");
        if (existing.isPresent()) {
            ArpIdentityMapping mapping = existing.get();
            if (!mapping.isConsentEnabled()) {
                log.info("Consent disabled for customer {}", customerId);
                return Optional.empty();
            }
            cacheToken(customerId, mapping.getArpToken());
            return Optional.of(mapping.getArpToken());
        }

        String newToken = mintToken();
        ArpIdentityMapping mapping = ArpIdentityMapping.builder()
                .customerId(customerId)
                .userId(userId)
                .arpToken(newToken)
                .privacyMode("BALANCED")
                .consentEnabled(true)
                .saltVersion(1)
                .status("ACTIVE")
                .expiresAt(LocalDateTime.now().plusDays(90))
                .createdAt(LocalDateTime.now())
                .build();
        mappingRepo.save(mapping);

        auditLogRepo.save(PrivacyAuditLog.builder()
                .customerId(customerId)
                .arpToken(newToken)
                .auditAction("TOKEN_MINTED")
                .fieldsStripped("customerId,userId,email")
                .createdAt(LocalDateTime.now())
                .build()
        );

        cacheToken(customerId, newToken);
        log.info("Minted new token {} for customer {}", newToken, customerId);
        return Optional.of(newToken);
    }

    /**
     * Resolves arpToken back to real customerId (reverse lookup).
     * Only called inside Trust Zone B — never expose to AI service (:8083).
     *
     * @param arpToken ephemeral token e.g. R789-9a4f8b21
     * @return Optional customerId
     */
    @Transactional(readOnly = true)
    public Optional<Long> resolveCustomerId(String arpToken) {
        String reverseKey = ARP_REVERSE_PREFIX + arpToken;
        String cachedCustId = redisTemplate.opsForValue().get(reverseKey);
        if (cachedCustId != null) {
            return Optional.of(Long.parseLong(cachedCustId));
        }

        return mappingRepo.findByArpTokenAndStatus(arpToken, "ACTIVE").map(m -> {
            redisTemplate.opsForValue().set(reverseKey, m.getCustomerId().toString(), Duration.ofHours(1));
            return m.getCustomerId();
        });
    }

    /**
     * Rotates the ARP token: marks current ACTIVE as ROTATED, mints a new ACTIVE token.
     * Old cached entries are evicted from Redis immediately.
     *
     * @param customerId real customer ID
     * @return newly minted arpToken string
     */
    @Transactional
    public String rotateToken(Long customerId) {
        mappingRepo.findByCustomerIdAndStatus(customerId, "ACTIVE").ifPresent(m -> {
            m.setStatus("ROTATED");
            m.setRevokedAt(LocalDateTime.now());
            mappingRepo.save(m);
            evictCache(customerId, m.getArpToken());
        });

        String newToken = mintToken();
        ArpIdentityMapping mapping = ArpIdentityMapping.builder()
                .customerId(customerId)
                .userId("ROTATED")
                .arpToken(newToken)
                .privacyMode("BALANCED")
                .consentEnabled(true)
                .saltVersion(1)
                .status("ACTIVE")
                .expiresAt(LocalDateTime.now().plusDays(90))
                .createdAt(LocalDateTime.now())
                .build();
        mappingRepo.save(mapping);
        cacheToken(customerId, newToken);

        auditLogRepo.save(PrivacyAuditLog.builder()
                .customerId(customerId)
                .arpToken(newToken)
                .auditAction("ROTATED")
                .fieldsStripped("all")
                .createdAt(LocalDateTime.now())
                .build());

        log.info("Rotated ARP token for customer {}: new token is {}", customerId, newToken);
        return newToken;
    }

    /**
     * Right-to-Erasure: marks mapping REVOKED, evicts Redis caches,
     * and publishes arp.identity.revoked tombstone via Kafka.
     * Spring AI service will delete all data for this arpToken upon receiving tombstone.
     *
     * @param customerId real customer ID
     */
    @Transactional
    public void revokeAndForget(Long customerId) {
        mappingRepo.findByCustomerIdAndStatus(customerId, "ACTIVE").ifPresent(m -> {
            m.setStatus("REVOKED");
            m.setRevokedAt(LocalDateTime.now());
            mappingRepo.save(m);

            evictCache(customerId, m.getArpToken());
            redisTemplate.delete("guard:policy:" + customerId);

            producer.publishIdentityRevoked(ArpIdentityRevokedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .arpToken(m.getArpToken())
                    .reason("USER_REVOKED_CONSENT")
                    .revokedAt(LocalDateTime.now())
                    .build());

            auditLogRepo.save(PrivacyAuditLog.builder()
                    .customerId(customerId)
                    .arpToken(m.getArpToken())
                    .auditAction("REVOKED")
                    .fieldsStripped("ALL_DATA_PURGED")
                    .createdAt(LocalDateTime.now())
                    .build());

            log.warn("Revoked and purged ARP identity for customer {}", customerId);
        });
    }

    private String mintToken() {
        int prefix = 100 + RANDOM.nextInt(900); // 100-999
        int hex1 = RANDOM.nextInt(0x10000);
        int hex2 = RANDOM.nextInt(0x10000);
        return String.format("R%d-%04x%04x", prefix, hex1, hex2);
    }

    private void cacheToken(Long customerId, String arpToken) {
        redisTemplate.opsForValue().set(ARP_FORWARD_PREFIX + customerId, arpToken, CACHE_TTL);
        redisTemplate.opsForValue().set(ARP_REVERSE_PREFIX + arpToken, customerId.toString(), CACHE_TTL);
    }

    private void evictCache(Long customerId, String arpToken) {
        redisTemplate.delete(ARP_FORWARD_PREFIX + customerId);
        redisTemplate.delete(ARP_REVERSE_PREFIX + arpToken);
    }
}
