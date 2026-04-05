package com.example.guangzhouorder.dto.chat;

import com.example.guangzhouorder.entity.PriceQuote;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceQuoteResponse {

    private Long quoteId;
    private Long orderId;
    private Long messageId;
    private String proposedByEmail;
    private String proposedByName;
    private String proposedByRole;
    private BigDecimal proposedPrice;
    private BigDecimal estimatePrice;
    private String note;
    private String status;
    private LocalDateTime createdAt;

    public static PriceQuoteResponse from(PriceQuote quote) {
        return PriceQuoteResponse.builder()
                .quoteId(quote.getQuoteId())
                .orderId(quote.getOrder().getOrderId())
                .messageId(quote.getMessage().getMessageId())
                .proposedByEmail(quote.getProposedBy().getEmail())
                .proposedByName(quote.getProposedBy().getName())
                .proposedByRole(quote.getProposedBy().getRole())
                .proposedPrice(quote.getProposedPrice())
                .estimatePrice(quote.getEstimatePrice())
                .note(quote.getNote())
                .status(quote.getStatus())
                .createdAt(quote.getCreatedAt())
                .build();
    }
}
