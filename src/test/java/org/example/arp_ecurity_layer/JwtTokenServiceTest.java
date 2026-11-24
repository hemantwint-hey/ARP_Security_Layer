package org.example.arp_ecurity_layer;

import tools.jackson.databind.ObjectMapper;
import org.example.arp_ecurity_layer.security.JwtTokenService;
import org.example.arp_ecurity_layer.security.JwtUserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenServiceTest {

    private JwtTokenService jwtTokenService;
    private final String secret = "test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha256";

    @BeforeEach
    void setUp() {
        jwtTokenService = new JwtTokenService(secret, new ObjectMapper());
    }

    @Test
    @DisplayName("Successfully generate and parse valid JWT token")
    void shouldGenerateAndParseToken() {
        String token = jwtTokenService.generateToken(123L, "user-abc-123", "user@bank.test", List.of("USER"), 3600);
        assertNotNull(token);

        Optional<JwtUserPrincipal> principalOpt = jwtTokenService.validateAndParse(token);
        assertTrue(principalOpt.isPresent());

        JwtUserPrincipal principal = principalOpt.get();
        assertEquals(123L, principal.getCustomerId());
        assertEquals("user-abc-123", principal.getUserId());
        assertEquals("user@bank.test", principal.getEmail());
    }

    @Test
    @DisplayName("Tampered signature should fail validation")
    void shouldRejectTamperedToken() {
        String token = jwtTokenService.generateToken(123L, "user-abc", "user@bank.test", List.of("USER"), 3600);
        String tampered = token.substring(0, token.lastIndexOf('.')) + ".invalidSignature";

        Optional<JwtUserPrincipal> principalOpt = jwtTokenService.validateAndParse(tampered);
        assertTrue(principalOpt.isEmpty());
    }

    @Test
    @DisplayName("Expired token should be rejected")
    void shouldRejectExpiredToken() {
        String expiredToken = jwtTokenService.generateToken(123L, "user-abc", "user@bank.test", List.of("USER"), -10);

        Optional<JwtUserPrincipal> principalOpt = jwtTokenService.validateAndParse(expiredToken);
        assertTrue(principalOpt.isEmpty());
    }
}
