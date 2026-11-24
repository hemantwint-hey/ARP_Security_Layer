package org.example.arp_ecurity_layer.web;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.arp_ecurity_layer.guard.entity.CustomerSavingPreference;
import org.example.arp_ecurity_layer.guard.entity.SavingGuardMetric;
import org.example.arp_ecurity_layer.guard.repository.CustomerSavingPreferenceRepository;
import org.example.arp_ecurity_layer.guard.repository.SavingGuardMetricRepository;
import org.example.arp_ecurity_layer.guard.service.SavingGuardSyncService;
import org.example.arp_ecurity_layer.security.SecurityUtils;
import org.example.arp_ecurity_layer.web.dto.SavingModeSummaryResponse;
import org.example.arp_ecurity_layer.web.dto.SavingPreferenceRequest;
import org.example.arp_ecurity_layer.web.dto.SavingPreferenceResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

@RestController
@RequestMapping("/api/v1/saving-mode")
@RequiredArgsConstructor
@Slf4j
public class SavingModeController {

    private final SavingGuardSyncService guardSyncService;
    private final CustomerSavingPreferenceRepository prefRepo;
    private final SavingGuardMetricRepository metricRepo;
    private final ObjectMapper objectMapper;

    /**
     * PUT /api/v1/saving-mode/preferences
     * Configures guard level, spending caps, and blocked categories.
     * Immediately syncs updated policy to Redis (banking monolith reads < 2ms).
     */
    @PutMapping("/preferences")
    public ResponseEntity<SavingPreferenceResponse> updatePreferences(
            @RequestBody SavingPreferenceRequest request, Authentication auth) {
        Long customerId = SecurityUtils.getCustomerId(auth);

        String guardLevel = request.getGuardLevel() != null ? request.getGuardLevel().toUpperCase() : "OFF";
        if (!Set.of("OFF", "SOFT_GUARD", "STRICT_LOCK").contains(guardLevel)) {
            guardLevel = "OFF";
        }

        List<String> categories = request.getBlockedCategories() != null ? request.getBlockedCategories() : Collections.emptyList();
        String categoriesJson;
        try {
            categoriesJson = objectMapper.writeValueAsString(categories);
        } catch (Exception e) {
            categoriesJson = "[]";
        }

        CustomerSavingPreference pref = prefRepo.findById(customerId).orElseGet(() ->
                CustomerSavingPreference.builder().customerId(customerId).build()
        );

        pref.setGuardLevel(guardLevel);
        pref.setMonthlyDiscretionaryCap(request.getMonthlyDiscretionaryCap());
        pref.setSingleTxSoftCap(request.getSingleTxSoftCap());
        pref.setBlockedCategoriesJson(categoriesJson);
        pref.setFailClosedOnOutage(request.isFailClosedOnOutage());
        pref.setUpdatedAt(LocalDateTime.now());

        CustomerSavingPreference saved = prefRepo.save(pref);
        guardSyncService.syncPreferenceOnlyToRedis(customerId, saved);

        return ResponseEntity.ok(SavingPreferenceResponse.builder()
                .customerId(customerId)
                .guardLevel(saved.getGuardLevel())
                .monthlyDiscretionaryCap(saved.getMonthlyDiscretionaryCap())
                .singleTxSoftCap(saved.getSingleTxSoftCap())
                .blockedCategories(categories)
                .failClosedOnOutage(saved.isFailClosedOnOutage())
                .updatedAt(saved.getUpdatedAt())
                .build());
    }

    /**
     * GET /api/v1/saving-mode/summary?period=2026-11
     * Returns money saved by AI Guard for the given period.
     */
    @GetMapping("/summary")
    public ResponseEntity<SavingModeSummaryResponse> getSummary(
            @RequestParam String period, Authentication auth) {
        Long customerId = SecurityUtils.getCustomerId(auth);

        YearMonth yearMonth;
        try {
            yearMonth = YearMonth.parse(period);
        } catch (Exception e) {
            yearMonth = YearMonth.now();
        }

        LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime end = yearMonth.atEndOfMonth().atTime(23, 59, 59, 999999999);

        List<SavingGuardMetric> metrics = metricRepo.findByCustomerIdAndCreatedAtBetween(customerId, start, end);

        BigDecimal totalSaved = metrics.stream()
                .map(SavingGuardMetric::getSavedAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long blockedCount = metrics.stream()
                .filter(m -> "BLOCKED".equalsIgnoreCase(m.getActionTaken()))
                .count();

        Optional<CustomerSavingPreference> prefOpt = prefRepo.findById(customerId);
        String guardLevel = prefOpt.map(CustomerSavingPreference::getGuardLevel).orElse("OFF");
        List<String> activeBlocked = prefOpt.map(p -> parseCategories(p.getBlockedCategoriesJson())).orElse(Collections.emptyList());

        return ResponseEntity.ok(SavingModeSummaryResponse.builder()
                .guardLevel(guardLevel)
                .totalSavedThisMonth(totalSaved)
                .blockedTransactionsCount(blockedCount)
                .activeBlockedCategories(activeBlocked)
                .build());
    }

    private List<String> parseCategories(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            JsonNode node = objectMapper.readTree(json);
            List<String> list = new ArrayList<>();
            if (node.isArray()) {
                node.forEach(item -> list.add(item.asText()));
            }
            return list;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
