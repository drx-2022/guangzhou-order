package com.example.guangzhouorder.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class AffiliateReferralDto {
    private Long productCardId;
    private String sku;
    private String name;
    private String imageUrl;
    private String categoryName;
    private String commissionLabel;
    private BigDecimal displayPrice;
    private String referralUrl;
}
