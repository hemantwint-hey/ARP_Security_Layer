package org.example.arp_ecurity_layer.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.arp_ecurity_layer.privacy.entity.ArpIdentityMapping;
import org.example.arp_ecurity_layer.privacy.repository.ArpIdentityMappingRepository;
import org.example.arp_ecurity_layer.privacy.service.ArpTokenResolverService;
import org.example.arp_ecurity_layer.security.JwtUserPrincipal;
import org.example.arp_ecurity_layer.security.SecurityUtils;
import org.example.arp_ecurity_layer.web.dto.ConsentRequest;
import org.example.arp_ecurity_layer.web.dto.ConsentResponse;
import org.example.arp_ecurity_layer.web.dto.IdentityRotateResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/privacy")
@RequiredArgsConstructor
@Slf4j
public class PrivacyConsentController {

    private final ArpTokenResolverService tokenResolver;
    private final ArpIdentityMappingRepository mappingRepo;
    private final StringRedisTemplate redisTemplate;

    /**
     * PUT /api/v1/privacy/consent
     * Opt-in/out of AI analytics and set privacy strictness level.
     * Body: { "consentEnabled": true, "privacyMode": "STRICT" }
     * Response: { "arpToken": "R789-...", "privacyMode": "STRICT", "consentEnabled": true }
     */
    @PutMapping("/consent")
    public ResponseEntity<ConsentResponse> updateConsent(@RequestBody ConsentRequest consentRequest, Authentication auth) {
        JwtUserPrincipal principal = SecurityUtils.getPrincipal(auth);
        Long customerId = principal.getCustomerId();
        String userId = principal.getUserId() != null ? principal.getUserId() : String.valueOf(customerId);

        ArpIdentityMapping mapping = mappingRepo.findByCustomerIdAndStatus(customerId, "ACTIVE")
                .orElseGet(() -> {
                    String token = tokenResolver.getOrCreateActiveToken(customerId, userId).orElse("");
                    return mappingRepo.findByCustomerIdAndStatus(customerId, "ACTIVE")
                            .orElse(ArpIdentityMapping.builder()
                                    .customerId(customerId)
                                    .userId(userId)
                                    .arpToken(token)
                                    .status("ACTIVE")
                                    .expiresAt(LocalDateTime.now().plusDays(90))
                                    .createdAt(LocalDateTime.now())
                                    .build());
                });

        mapping.setConsentEnabled(consentRequest.isConsentEnabled());
        if (consentRequest.getPrivacyMode() != null && !consentRequest.getPrivacyMode().isBlank()) {
            mapping.setPrivacyMode(consentRequest.getPrivacyMode().toUpperCase());
        }
        mappingRepo.save(mapping);

        // Evict Redis cache: arp:map:{customerId}
        redisTemplate.delete("arp:map:" + customerId);
        if (!consentRequest.isConsentEnabled()) {
            redisTemplate.delete("guard:policy:" + customerId);
        }

        String tokenPrefix = (mapping.getArpToken() != null && mapping.getArpToken().length() >= 4)
                ? mapping.getArpToken().substring(0, Math.min(8, mapping.getArpToken().length())) + "..."
                : null;

        return ResponseEntity.ok(ConsentResponse.builder()
                .arpToken(tokenPrefix)
                .privacyMode(mapping.getPrivacyMode())
                .consentEnabled(mapping.isConsentEnabled())
                .build());
    }

    /**
     * POST /api/v1/privacy/identity/rotate
     * Rotates the ephemeral ARP token for the authenticated customer.
     * Response: { "previousTokenStatus": "ROTATED", "newTokenPrefix": "R412-..." }
     */
    @PostMapping("/identity/rotate")
    public ResponseEntity<IdentityRotateResponse> rotateIdentity(Authentication auth) {
        Long customerId = SecurityUtils.getCustomerId(auth);
        String newToken = tokenResolver.rotateToken(customerId);

        String prefix = newToken.substring(0, Math.min(8, newToken.length())) + "...";
        return ResponseEntity.ok(IdentityRotateResponse.builder()
                .previousTokenStatus("ROTATED")
                .newTokenPrefix(prefix)
                .build());
    }

    /**
     * POST /api/v1/privacy/identity/revoke
     * "Forget Me": revokes consent and erases all AI-linked data via Kafka tombstone.
     * Response: 204 No Content
     */
    @PostMapping("/identity/revoke")
    public ResponseEntity<Void> revokeIdentity(Authentication auth) {
        Long customerId = SecurityUtils.getCustomerId(auth);
        tokenResolver.revokeAndForget(customerId);
        return ResponseEntity.noContent().build();
    }
}
