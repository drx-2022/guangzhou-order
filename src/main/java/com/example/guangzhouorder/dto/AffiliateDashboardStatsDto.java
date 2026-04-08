package com.example.guangzhouorder.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class AffiliateDashboardStatsDto {
    private long totalClicks;
    private double conversionRate;
    private BigDecimal totalCommission;
    private BigDecimal pendingRewards;
}
