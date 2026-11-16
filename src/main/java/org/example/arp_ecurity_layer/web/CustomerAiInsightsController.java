package org.example.arp_ecurity_layer.web;

import lombok.RequiredArgsConstructor;
import org.example.arp_ecurity_layer.insights.repository.ResolvedCustomerInsightRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai-insights")
@RequiredArgsConstructor
public class CustomerAiInsightsController {

    private final ResolvedCustomerInsightRepository insightRepo;

    /**
     * GET /api/v1/ai-insights?page=0&size=20&sort=createdAt,desc
     * Returns paginated AI-generated savings insights for the authenticated customer.
     */
    @GetMapping
    public ResponseEntity<?> getMyInsights(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable,
            Authentication auth) {
        // TODO: Extract customerId from JWT authentication principal
        // TODO: insightRepo.findByCustomerId(customerId, pageable)
        // TODO: Map Page<ResolvedCustomerInsight> to Page of InsightResponse DTO
        // TODO: Return wrapped in PagedResponse envelope
        throw new UnsupportedOperationException("TODO: implement");
    }

    /**
     * PATCH /api/v1/ai-insights/{id}/acknowledge
     * Marks an AI insight as read/dismissed by the customer.
     */
    @PatchMapping("/{id}/acknowledge")
    public ResponseEntity<?> acknowledgeInsight(@PathVariable Long id, Authentication auth) {
        // TODO: Extract customerId from JWT
        // TODO: Load insight by id — throw 404 if not found
        // TODO: IDOR check: verify insight.getCustomerId() == authenticated customerId (throw 403 if mismatch)
        // TODO: Set acknowledged = true, save
        // TODO: Return 200 OK
        throw new UnsupportedOperationException("TODO: implement");
    }
}

