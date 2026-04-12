package com.example.guangzhouorder.dto.chat;

import com.example.guangzhouorder.entity.VisualProof;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisualProofResponse {

    private Long visualProofId;
    private Long conversationId;
    private Long messageId;
    private String imageUrl;
    private String status;
    private String sentByEmail;
    private String sentByName;
    private LocalDateTime decidedAt;
    private LocalDateTime createdAt;

    public static VisualProofResponse from(VisualProof visualProof) {
        return VisualProofResponse.builder()
                .visualProofId(visualProof.getVisualProofId())
                .conversationId(visualProof.getConversation().getConversationId())
                .messageId(visualProof.getMessage().getMessageId())
                .imageUrl(visualProof.getImageUrl())
                .status(visualProof.getStatus())
                .sentByEmail(visualProof.getSentBy().getEmail())
                .sentByName(visualProof.getSentBy().getName())
                .decidedAt(visualProof.getDecidedAt())
                .createdAt(visualProof.getCreatedAt())
                .build();
    }
}

