package org.example.arp_ecurity_layer.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavingModeSummaryResponse {
    private String guardLevel;
    private BigDecimal totalSavedThisMonth;
    private long blockedTransactionsCount;
    private List<String> activeBlockedCategories;
}
