package org.example.arp_ecurity_layer.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.arp_ecurity_layer.insights.entity.ResolvedCustomerInsight;
import org.example.arp_ecurity_layer.insights.repository.ResolvedCustomerInsightRepository;
import org.example.arp_ecurity_layer.security.SecurityUtils;
import org.example.arp_ecurity_layer.web.dto.PagedResponse;
import org.example.arp_ecurity_layer.web.dto.ResolvedInsightResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai-insights")
@RequiredArgsConstructor
@Slf4j
public class CustomerAiInsightsController {

    private final ResolvedCustomerInsightRepository insightRepo;

    /**
     * GET /api/v1/ai-insights?page=0&size=20&sort=createdAt,desc
     * Returns paginated AI-generated savings insights for the authenticated customer.
     */
    @GetMapping
    public ResponseEntity<PagedResponse<ResolvedInsightResponse>> getMyInsights(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable,
            Authentication auth) {
        Long customerId = SecurityUtils.getCustomerId(auth);

        Page<ResolvedCustomerInsight> page = insightRepo.findByCustomerId(customerId, pageable);

        List<ResolvedInsightResponse> content = page.getContent().stream()
                .map(ResolvedInsightResponse::fromEntity)
                .toList();

        return ResponseEntity.ok(PagedResponse.<ResolvedInsightResponse>builder()
                .content(content)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build());
    }

    /**
     * PATCH /api/v1/ai-insights/{id}/acknowledge
     * Marks an AI insight as read/dismissed by the customer.
     */
    @PatchMapping("/{id}/acknowledge")
    public ResponseEntity<Map<String, Object>> acknowledgeInsight(@PathVariable Long id, Authentication auth) {
        Long customerId = SecurityUtils.getCustomerId(auth);

        ResolvedCustomerInsight insight = insightRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Insight not found with id: " + id));

        // IDOR check: verify insight.getCustomerId() == authenticated customerId
        if (!insight.getCustomerId().equals(customerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: cannot modify insight belonging to another customer");
        }

        insight.setAcknowledged(true);
        insightRepo.save(insight);

        return ResponseEntity.ok(Map.of("message", "Insight acknowledged", "id", id));
    }
}
