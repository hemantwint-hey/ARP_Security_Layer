package org.example.arp_ecurity_layer.security;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

@Service
@Slf4j
public class JwtTokenService {

    private final String secretKey;
    private final ObjectMapper objectMapper;

    public JwtTokenService(
            @Value("${jwt.secret:enterprise-digital-banking-system-super-secure-jwt-secret-key-256-bit}") String secretKey,
            ObjectMapper objectMapper) {
        this.secretKey = secretKey;
        this.objectMapper = objectMapper;
    }

    /**
     * Validates and parses HMAC-SHA256 signed JWT.
     */
    public Optional<JwtUserPrincipal> validateAndParse(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                log.warn("Invalid JWT structure: parts count={}", parts.length);
                return Optional.empty();
            }

            String content = parts[0] + "." + parts[1];
            byte[] expectedSignature = hmacSha256(content, secretKey);
            byte[] providedSignature = Base64.getUrlDecoder().decode(parts[2]);

            if (!MessageDigest.isEqual(expectedSignature, providedSignature)) {
                log.warn("JWT signature verification failed");
                return Optional.empty();
            }

            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode claims = objectMapper.readTree(payloadBytes);

            if (claims.has("exp")) {
                long exp = claims.get("exp").asLong();
                if (Instant.now().getEpochSecond() > exp) {
                    log.warn("JWT has expired at {}", exp);
                    return Optional.empty();
                }
            }

            Long customerId = null;
            if (claims.has("customerId")) {
                customerId = claims.get("customerId").asLong();
            } else if (claims.has("customer_id")) {
                customerId = claims.get("customer_id").asLong();
            }

            String userId = null;
            if (claims.has("userId")) {
                userId = claims.get("userId").asText();
            } else if (claims.has("user_id")) {
                userId = claims.get("user_id").asText();
            } else if (claims.has("sub")) {
                userId = claims.get("sub").asText();
            }

            if (customerId == null && userId != null && userId.matches("\\d+")) {
                customerId = Long.parseLong(userId);
            }

            String email = claims.has("email") ? claims.get("email").asText() : null;

            List<String> roles = new ArrayList<>();
            if (claims.has("roles") && claims.get("roles").isArray()) {
                claims.get("roles").forEach(r -> roles.add(r.asText()));
            }

            return Optional.of(JwtUserPrincipal.builder()
                    .customerId(customerId)
                    .userId(userId)
                    .email(email)
                    .roles(roles)
                    .build());

        } catch (Exception e) {
            log.error("Failed to parse JWT: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Generates a signed token (useful for tests and mock auth).
     */
    public String generateToken(Long customerId, String userId, String email, List<String> roles, long expirationSeconds) {
        try {
            Map<String, Object> header = new HashMap<>();
            header.put("alg", "HS256");
            header.put("typ", "JWT");

            Map<String, Object> payload = new HashMap<>();
            payload.put("sub", userId != null ? userId : String.valueOf(customerId));
            if (customerId != null) payload.put("customerId", customerId);
            if (userId != null) payload.put("userId", userId);
            if (email != null) payload.put("email", email);
            if (roles != null) payload.put("roles", roles);
            payload.put("iat", Instant.now().getEpochSecond());
            payload.put("exp", Instant.now().getEpochSecond() + expirationSeconds);

            String headerB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(objectMapper.writeValueAsBytes(header));
            String payloadB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(objectMapper.writeValueAsBytes(payload));
            String content = headerB64 + "." + payloadB64;
            String signatureB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(hmacSha256(content, secretKey));

            return content + "." + signatureB64;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate JWT", e);
        }
    }

    private byte[] hmacSha256(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }
}
