package com.example.guangzhouorder.dto.chat;

import com.example.guangzhouorder.entity.SpecProposalCard;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class SpecProposalCardResponse {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Long proposalCardId;
    private Long conversationId;
    private Long messageId;
    private String materials;
    private String dimensions;
    private String technicalNotes;
    private List<String> referencePhotoUrls;
    private String status;
    private Long resultOrderId;
    private LocalDateTime createdAt;
    private String color;
    private Integer quantity;
    private BigDecimal estimatePrice;

    public static SpecProposalCardResponse from(SpecProposalCard card) {
        SpecProposalCardResponse response = SpecProposalCardResponse.builder()
                .proposalCardId(card.getProposalCardId())
                .conversationId(card.getConversation().getConversationId())
                .messageId(card.getMessage().getMessageId())
                .status(card.getStatus())
                .estimatePrice(card.getEstimatePrice())
                .resultOrderId(card.getResultOrderId())
                .createdAt(card.getCreatedAt())
                .build();

        try {
            JsonNode dna = MAPPER.readTree(card.getCardDna());
            response.setMaterials(dna.has("materials") ? dna.get("materials").asText() : null);
            response.setDimensions(dna.has("dimensions") ? dna.get("dimensions").asText() : null);
            response.setTechnicalNotes(dna.has("technicalNotes") ? dna.get("technicalNotes").asText() : null);

            List<String> urls = new ArrayList<>();
            if (dna.has("referencePhotoUrls") && dna.get("referencePhotoUrls").isArray()) {
                dna.get("referencePhotoUrls").forEach(node -> urls.add(node.asText()));
            }
            response.setReferencePhotoUrls(urls);
            response.setColor(dna.has("color") ? dna.get("color").asText() : null);
            response.setQuantity(dna.has("quantity") && !dna.get("quantity").isNull()
                    ? dna.get("quantity").asInt() : null);
        } catch (Exception e) {
            log.warn("Failed to parse cardDna for proposalCardId: {}", card.getProposalCardId());
            response.setMaterials(null);
            response.setDimensions(null);
            response.setTechnicalNotes(null);
            response.setReferencePhotoUrls(null);
            response.setColor(null);
            response.setQuantity(null);
        }

        return response;
    }
}