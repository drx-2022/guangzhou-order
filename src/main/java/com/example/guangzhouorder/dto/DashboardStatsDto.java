package com.example.guangzhouorder.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DashboardStatsDto {
    private long negotiating;
    private long inProduction;
    private long pendingApproval;
    private long completed;
}
