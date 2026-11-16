package org.example.arp_ecurity_layer.web;

import lombok.RequiredArgsConstructor;
import org.example.arp_ecurity_layer.guard.service.SavingGuardSyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/saving-mode")
@RequiredArgsConstructor
public class SavingModeController {

    private final SavingGuardSyncService guardSyncService;

    /**
     * PUT /api/v1/saving-mode/preferences
     * Configures guard level, spending caps, and blocked categories.
     * Immediately syncs updated policy to Redis (banking monolith reads < 2ms).
     * Body: { "guardLevel": "STRICT_LOCK", "monthlyDiscretionaryCap": 15000.00,
     *         "singleTxSoftCap": 2000.00, "blockedCategories": ["FOOD_DELIVERY","SHOPPING"],
     *         "failClosedOnOutage": false }
     */
    @PutMapping("/preferences")
    public ResponseEntity<?> updatePreferences(@RequestBody Object request, Authentication auth) {
        // TODO: Extract customerId from JWT
        // TODO: Validate guardLevel is one of: OFF | SOFT_GUARD | STRICT_LOCK
        // TODO: Serialize blockedCategories list to JSON string
        // TODO: Build and save CustomerSavingPreference entity (updatedAt = now)
        // TODO: Call guardSyncService.syncPreferenceOnlyToRedis(customerId, pref)
        // TODO: Return 200 OK with saved preference details
        throw new UnsupportedOperationException("TODO: implement");
    }

    /**
     * GET /api/v1/saving-mode/summary?period=2026-11
     * Returns money saved by AI Guard for the given period.
     * Response: { "guardLevel": "STRICT_LOCK", "totalSavedThisMonth": 4850.00,
     *             "blockedTransactionsCount": 3, "activeBlockedCategories": [...] }
     */
    @GetMapping("/summary")
    public ResponseEntity<?> getSummary(@RequestParam String period, Authentication auth) {
        // TODO: Extract customerId from JWT
        // TODO: Parse period (YYYY-MM) to date range: start = first day of month, end = last day
        // TODO: Inject SavingGuardMetricRepository and query metrics for customerId + period
        // TODO: Aggregate: sum(savedAmount), count(id), group by category
        // TODO: Load guardLevel from CustomerSavingPreference or Redis
        // TODO: Return summary response
        throw new UnsupportedOperationException("TODO: implement");
    }
}

