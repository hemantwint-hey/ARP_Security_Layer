package org.example.arp_ecurity_layer.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.arp_ecurity_layer.insights.entity.ResolvedCustomerInsight;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResolvedInsightResponse {
    private Long id;
    private Long customerId;
    private String sourceEventId;
    private String insightType;
    private String severity;
    private String category;
    private String title;
    private String recommendation;
    private BigDecimal estimatedMonthlySavings;
    private boolean acknowledged;
    private LocalDateTime createdAt;

    public static ResolvedInsightResponse fromEntity(ResolvedCustomerInsight entity) {
        return ResolvedInsightResponse.builder()
                .id(entity.getId())
                .customerId(entity.getCustomerId())
                .sourceEventId(entity.getSourceEventId())
                .insightType(entity.getInsightType())
                .severity(entity.getSeverity())
                .category(entity.getCategory())
                .title(entity.getTitle())
                .recommendation(entity.getRecommendation())
                .estimatedMonthlySavings(entity.getEstimatedMonthlySavings())
                .acknowledged(entity.isAcknowledged())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
