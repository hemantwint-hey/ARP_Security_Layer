package org.example.arp_ecurity_layer.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavingPreferenceResponse {
    private Long customerId;
    private String guardLevel;
    private BigDecimal monthlyDiscretionaryCap;
    private BigDecimal singleTxSoftCap;
    private List<String> blockedCategories;
    private boolean failClosedOnOutage;
    private LocalDateTime updatedAt;
}
