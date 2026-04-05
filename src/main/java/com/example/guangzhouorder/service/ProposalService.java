package com.example.guangzhouorder.service;

import com.example.guangzhouorder.dto.chat.MessageResponse;
import com.example.guangzhouorder.dto.chat.SpecProposalCardRequest;
import com.example.guangzhouorder.dto.chat.SpecProposalCardResponse;
import com.example.guangzhouorder.entity.*;
import com.example.guangzhouorder.repository.ConversationRepository;
import com.example.guangzhouorder.repository.MessageRepository;
import com.example.guangzhouorder.repository.OrderRepository;
import com.example.guangzhouorder.repository.SpecProposalCardRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProposalService {

    private final SpecProposalCardRepository specProposalCardRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final OrderRepository orderRepository;
    private final ChatService chatService;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public SpecProposalCardResponse createProposalCard(Long conversationId, SpecProposalCardRequest request, User admin) {
        if (!"ADMIN".equals(admin.getRole())) {
            throw new AccessDeniedException("Only admins can create proposal cards.");
        }
        Conversation conversation = chatService.getConversationById(conversationId, admin);
        if (specProposalCardRepository.existsByConversationAndStatus(conversation, "PENDING")) {
            throw new IllegalStateException("There is already a pending proposal card for this conversation.");
        }
        String cardDnaJson;
        Map<String, Object> dnaMap = new LinkedHashMap<>();
        dnaMap.put("dimensions", request.getDimensions());
        dnaMap.put("materials", request.getMaterials());
        dnaMap.put("color", request.getColor());
        dnaMap.put("quantity", request.getQuantity());
        dnaMap.put("technicalNotes", request.getTechnicalNotes());
        dnaMap.put("referencePhotoUrls", request.getReferencePhotoUrls());
        try {
            cardDnaJson = objectMapper.writeValueAsString(dnaMap);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize proposal card DNA", e);
        }

        MessageResponse msgResponse = chatService.sendMessage(conversationId, "SPEC_PROPOSAL_CARD", null, null, admin);

        Message msg = messageRepository.findById(msgResponse.getMessageId()).orElseThrow(() -> new RuntimeException("Message not found after save"));
        SpecProposalCard card = SpecProposalCard.builder()
                .message(msg)
                .conversation(conversation)
                .cardDna(cardDnaJson)
                .estimatePrice(request.getEstimatePrice())
                .status("PENDING")
                .build();

        SpecProposalCard savedCard = specProposalCardRepository.save(card);

        SpecProposalCardResponse response = SpecProposalCardResponse.from(savedCard);

        MessageResponse broadcastMsg = MessageResponse.builder()
                .messageId(msgResponse.getMessageId())
                .senderEmail(admin.getEmail())
                .senderName(admin.getName())
                .senderRole(admin.getRole())
                .conversationId(conversationId)
                .messageType("SPEC_PROPOSAL_CARD")
                .createdAt(msgResponse.getCreatedAt())
                .proposalCard(response)
                .build();

        simpMessagingTemplate.convertAndSend("/topic/conversation." + conversationId, broadcastMsg);
        return response;
    }

    @Transactional
    public SpecProposalCardResponse acceptProposalCard(Long proposalCardId, User customer) {
        SpecProposalCard card = specProposalCardRepository.findById(proposalCardId).orElseThrow(() -> new IllegalArgumentException("Proposal card not found"));
        if (!card.getConversation().getCustomer().getUserId().equals(customer.getUserId())) {
            throw new AccessDeniedException("Access denied");
        }
        if (!"PENDING".equals(card.getStatus())) {
            throw new IllegalStateException("This proposal has already been " + card.getStatus().toLowerCase() + ".");
        }

        Order order = Order.builder()
                .customer(customer)
                .structuredSpecs(card.getCardDna())
                .status("Negotiating Price")
                .build();
        Order saveOrder = orderRepository.save(order);

        card.setStatus("ACCEPTED");
        card.setResultOrderId(saveOrder.getOrderId());
        specProposalCardRepository.save(card);

        SpecProposalCardResponse response = SpecProposalCardResponse.from(card);
        Map<String, Object> updatePayload = new LinkedHashMap<>();
        updatePayload.put("eventType", "PROPOSAL_STATUS_UPDATE");
        updatePayload.put("proposalCardId", proposalCardId);
        updatePayload.put("status", "ACCEPTED");
        updatePayload.put("resultOrderId", saveOrder.getOrderId());
        Object payload = updatePayload;
        simpMessagingTemplate.convertAndSend("/topic/conversation." + card.getConversation().getConversationId(), payload);

        return response;

    }

    @Transactional
    public SpecProposalCardResponse declineProposalCard(Long proposalCardId, User customer) {
        SpecProposalCard card = specProposalCardRepository.findById(proposalCardId)
                .orElseThrow(() -> new IllegalArgumentException("Proposal card not found"));

        if (!card.getConversation().getCustomer().getUserId().equals(customer.getUserId())) {
            throw new AccessDeniedException("Access denied");
        }

        if (!"PENDING".equals(card.getStatus())) {
            throw new IllegalStateException("This proposal has already been " + card.getStatus().toLowerCase() + ".");
        }

        card.setStatus("DECLINED");
        specProposalCardRepository.save(card);

        SpecProposalCardResponse response = SpecProposalCardResponse.from(card);

        Map<String, Object> updatePayload = new LinkedHashMap<>();
        updatePayload.put("eventType", "PROPOSAL_STATUS_UPDATE");
        updatePayload.put("proposalCardId", proposalCardId);
        updatePayload.put("status", "DECLINED");
        updatePayload.put("resultOrderId", null);

        Object payload = updatePayload;

        simpMessagingTemplate.convertAndSend("/topic/conversation." + card.getConversation().getConversationId(), payload);

        return response;
    }

    public SpecProposalCardResponse getProposalCard(Long proposalCardId, User requestingUser) {
        SpecProposalCard card = specProposalCardRepository.findById(proposalCardId)
                .orElseThrow(() -> new IllegalArgumentException("Proposal card not found"));

        boolean isAdmin = "ADMIN".equals(requestingUser.getRole());
        boolean isOwner = card.getConversation().getCustomer().getUserId().equals(requestingUser.getUserId());

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("Access denied");
        }

        return SpecProposalCardResponse.from(card);
    }

    public SpecProposalCardResponse getProposalCardByMessageId(Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));
        SpecProposalCard card = specProposalCardRepository.findByMessage(message)
                .orElseThrow(() -> new IllegalArgumentException("Proposal card not found"));
        return SpecProposalCardResponse.from(card);
    }

}