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
public class SavingPreferenceRequest {
    private String guardLevel; // OFF | SOFT_GUARD | STRICT_LOCK
    private BigDecimal monthlyDiscretionaryCap;
    private BigDecimal singleTxSoftCap;
    private List<String> blockedCategories;
    private boolean failClosedOnOutage;
}
