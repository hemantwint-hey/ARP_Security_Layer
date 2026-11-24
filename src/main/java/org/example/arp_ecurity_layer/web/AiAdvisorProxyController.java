package org.example.arp_ecurity_layer.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.arp_ecurity_layer.privacy.service.ArpTokenResolverService;
import org.example.arp_ecurity_layer.privacy.service.PiiScrubber;
import org.example.arp_ecurity_layer.security.JwtUserPrincipal;
import org.example.arp_ecurity_layer.security.SecurityUtils;
import org.example.arp_ecurity_layer.web.dto.AdvisorAnswerResponse;
import org.example.arp_ecurity_layer.web.dto.AdvisorQuestionRequest;
import org.example.arp_ecurity_layer.web.dto.InternalAdvisorRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/ai-advisor")
@RequiredArgsConstructor
@Slf4j
public class AiAdvisorProxyController {

    private final ArpTokenResolverService tokenResolver;
    private final PiiScrubber piiScrubber;

    @Value("${arp.ai-service.base-url:http://localhost:8083}")
    private String aiServiceBaseUrl;

    /**
     * POST /api/v1/ai-advisor/ask
     * Privacy-safe natural-language Q&A gateway.
     * Translates customerId -> arpToken BEFORE forwarding to Spring AI internal API (:8083).
     * Body: { "question": "Where can I cut 3000 monthly?" }
     * Response: { "answer": "...", "highlightedCategories": [...], "estimatedSavings": 5200.00 }
     */
    @PostMapping("/ask")
    public ResponseEntity<?> askAdvisor(@RequestBody AdvisorQuestionRequest request, Authentication auth) {
        JwtUserPrincipal principal = SecurityUtils.getPrincipal(auth);
        Long customerId = principal.getCustomerId();
        String userId = principal.getUserId() != null ? principal.getUserId() : String.valueOf(customerId);

        if (request == null || request.getQuestion() == null || request.getQuestion().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Question cannot be empty"));
        }

        // 1. Strip prompt injection & scrub accidental PII
        String stripped = piiScrubber.stripPromptInjection(request.getQuestion());
        PiiScrubber.ScrubResult scrubResult = piiScrubber.scrubFreeText(stripped);
        String sanitizedQuestion = scrubResult.scrubbedText();

        // 2. Resolve customerId -> arpToken
        Optional<String> arpTokenOpt = tokenResolver.getOrCreateActiveToken(customerId, userId);
        if (arpTokenOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "AI advisor requires privacy consent to be enabled"));
        }
        String arpToken = arpTokenOpt.get();

        // 3. Call Spring AI internal API (:8083)
        try {
            RestClient restClient = RestClient.builder().baseUrl(aiServiceBaseUrl).build();
            InternalAdvisorRequest internalRequest = InternalAdvisorRequest.builder()
                    .arpToken(arpToken)
                    .sanitizedQuestion(sanitizedQuestion)
                    .build();

            AdvisorAnswerResponse response = restClient.post()
                    .uri("/internal/v1/advisor/ask")
                    .body(internalRequest)
                    .retrieve()
                    .body(AdvisorAnswerResponse.class);

            if (response == null) {
                return ResponseEntity.ok(AdvisorAnswerResponse.builder()
                        .answer("Based on your recent spending analysis, no critical budget leaks were detected.")
                        .highlightedCategories(Collections.emptyList())
                        .estimatedSavings(BigDecimal.ZERO)
                        .build());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.warn("Spring AI advisor service at {} is unavailable or returned error: {}", aiServiceBaseUrl, e.getMessage());
            // Graceful fallback when local Ollama/AI service is initializing or unreachable
            return ResponseEntity.ok(AdvisorAnswerResponse.builder()
                    .answer("AI Advisor is currently analyzing your anonymized spend trends. Please check back shortly or review your active savings guard rules.")
                    .highlightedCategories(Collections.emptyList())
                    .estimatedSavings(BigDecimal.ZERO)
                    .build());
        }
    }
}
