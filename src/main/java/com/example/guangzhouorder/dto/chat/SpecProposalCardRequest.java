package com.example.guangzhouorder.dto.chat;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class SpecProposalCardRequest {

    @NotBlank(message = "Materials cannot be blank")
    private String materials;

    @NotBlank(message = "Dimensions cannot be blank")
    private String dimensions;

    private String technicalNotes;

    private List<String> referencePhotoUrls;

    @NotBlank(message = "Color cannot be blank")
    private String color;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @NotNull(message = "Estimate price is required")
    @DecimalMin(value = "0.01", message = "Estimate price must be greater than 0")
    private BigDecimal estimatePrice;
}