package org.example.arp_ecurity_layer.web;

import lombok.RequiredArgsConstructor;
import org.example.arp_ecurity_layer.privacy.service.ArpTokenResolverService;
import org.example.arp_ecurity_layer.privacy.service.PiiScrubber;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai-advisor")
@RequiredArgsConstructor
public class AiAdvisorProxyController {

    private final ArpTokenResolverService tokenResolver;
    private final PiiScrubber piiScrubber;

    /**
     * POST /api/v1/ai-advisor/ask
     * Privacy-safe natural-language Q&A gateway.
     * Translates customerId -> arpToken BEFORE forwarding to Spring AI internal API (:8083).
     * Body: { "question": "Where can I cut 3000 monthly?" }
     * Response: { "answer": "...", "highlightedCategories": [...], "estimatedSavings": 5200.00 }
     */
    @PostMapping("/ask")
    public ResponseEntity<?> askAdvisor(@RequestBody Object request, Authentication auth) {
        // TODO: Extract customerId and userId from JWT
        // TODO: Extract question string from request body
        // TODO: Strip prompt injection: piiScrubber.stripPromptInjection(question)
        // TODO: Scrub accidental PII: piiScrubber.scrubFreeText(sanitizedQuestion)
        // TODO: Resolve customerId -> arpToken via tokenResolver.getOrCreateActiveToken()
        // TODO: If arpToken empty (consent off) -> return 403 with message "AI advisor requires consent"
        // TODO: Call Spring AI internal API: POST http://localhost:8083/internal/v1/advisor/ask
        //       Body: { "arpToken": token, "sanitizedQuestion": question }
        //       (Use RestClient or WebClient — arpToken is NEVER returned to the customer)
        // TODO: Return advisor answer (strip arpToken from response before returning)
        throw new UnsupportedOperationException("TODO: implement");
    }
}

