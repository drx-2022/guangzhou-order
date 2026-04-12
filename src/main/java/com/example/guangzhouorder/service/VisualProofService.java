package com.example.guangzhouorder.service;

import com.example.guangzhouorder.dto.chat.MessageResponse;
import com.example.guangzhouorder.dto.chat.VisualProofResponse;
import com.example.guangzhouorder.entity.Conversation;
import com.example.guangzhouorder.entity.Message;
import com.example.guangzhouorder.entity.User;
import com.example.guangzhouorder.entity.VisualProof;
import com.example.guangzhouorder.repository.MessageRepository;
import com.example.guangzhouorder.repository.VisualProofRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VisualProofService {

    private final ChatService chatService;
    private final MessageRepository messageRepository;
    private final VisualProofRepository visualProofRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;

    @Transactional
    public MessageResponse sendVisualProof(Long conversationId, String imageUrl, User sender) {
        if (!"ADMIN".equals(sender.getRole())) {
            throw new AccessDeniedException("Only admins can send visual proofs.");
        }

        Conversation conversation = chatService.getConversationById(conversationId, sender);
        MessageResponse msgResponse = chatService.sendMessage(conversationId, "VISUAL_PROOF", null, imageUrl, sender);

        Message message = messageRepository.findById(msgResponse.getMessageId())
                .orElseThrow(() -> new IllegalStateException("Message not found after save"));

        VisualProof visualProof = VisualProof.builder()
                .message(message)
                .conversation(conversation)
                .sentBy(sender)
                .imageUrl(imageUrl)
                .status("PENDING")
                .build();

        VisualProof saved = visualProofRepository.save(visualProof);
        VisualProofResponse visualProofResponse = VisualProofResponse.from(saved);

        MessageResponse broadcast = MessageResponse.builder()
                .messageId(msgResponse.getMessageId())
                .senderEmail(sender.getEmail())
                .senderName(sender.getName())
                .senderRole(sender.getRole())
                .conversationId(conversationId)
                .messageType("VISUAL_PROOF")
                .mediaUrl(imageUrl)
                .createdAt(msgResponse.getCreatedAt())
                .visualProof(visualProofResponse)
                .build();

        simpMessagingTemplate.convertAndSend("/topic/conversation." + conversationId, broadcast);
        return broadcast;
    }

    @Transactional
    public VisualProofResponse acceptVisualProof(Long visualProofId, User customer) {
        VisualProof visualProof = loadAndValidateCustomerAction(visualProofId, customer);
        visualProof.setStatus("ACCEPTED");
        visualProof.setDecidedAt(LocalDateTime.now());
        visualProofRepository.save(visualProof);

        publishStatusUpdate(visualProof, "ACCEPTED");
        return VisualProofResponse.from(visualProof);
    }

    @Transactional
    public VisualProofResponse declineVisualProof(Long visualProofId, User customer) {
        VisualProof visualProof = loadAndValidateCustomerAction(visualProofId, customer);
        visualProof.setStatus("DECLINED");
        visualProof.setDecidedAt(LocalDateTime.now());
        visualProofRepository.save(visualProof);

        publishStatusUpdate(visualProof, "DECLINED");
        return VisualProofResponse.from(visualProof);
    }

    private VisualProof loadAndValidateCustomerAction(Long visualProofId, User customer) {
        VisualProof visualProof = visualProofRepository.findById(visualProofId)
                .orElseThrow(() -> new IllegalArgumentException("Visual proof not found"));

        if (!visualProof.getConversation().getCustomer().getUserId().equals(customer.getUserId())) {
            throw new AccessDeniedException("Access denied");
        }
        if (!"PENDING".equals(visualProof.getStatus())) {
            throw new IllegalStateException("This visual proof has already been " + visualProof.getStatus().toLowerCase() + ".");
        }
        return visualProof;
    }

    private void publishStatusUpdate(VisualProof visualProof, String status) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventType", "VISUAL_PROOF_STATUS_UPDATE");
        payload.put("visualProofId", visualProof.getVisualProofId());
        payload.put("status", status);
        simpMessagingTemplate.convertAndSend(
                "/topic/conversation." + visualProof.getConversation().getConversationId(),
                (Object) payload
        );
    }
}


