package com.example.guangzhouorder.dto.chat;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceQuoteRequest {

    @NotNull(message = "Order ID is required")
    private Long orderId;

    @NotNull(message = "Proposed price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal proposedPrice;

    private String note;
}
