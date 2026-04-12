package com.example.guangzhouorder.dto.chat;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VisualProofSendRequest {

    @NotBlank(message = "imageUrl is required")
    private String imageUrl;
}

