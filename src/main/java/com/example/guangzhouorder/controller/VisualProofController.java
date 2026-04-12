package com.example.guangzhouorder.controller;

import com.example.guangzhouorder.dto.chat.MessageResponse;
import com.example.guangzhouorder.dto.chat.VisualProofResponse;
import com.example.guangzhouorder.dto.chat.VisualProofSendRequest;
import com.example.guangzhouorder.entity.User;
import com.example.guangzhouorder.service.ChatService;
import com.example.guangzhouorder.service.VisualProofService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class VisualProofController {

    private final VisualProofService visualProofService;
    private final ChatService chatService;

    @PostMapping("/admin/chat/{conversationId}/visual-proof/send")
    public ResponseEntity<MessageResponse> sendVisualProof(
            @PathVariable Long conversationId,
            @Valid @RequestBody VisualProofSendRequest request) {

        User admin = chatService.getCurrentUser();
        if (!"ADMIN".equals(admin.getRole())) {
            throw new AccessDeniedException("Access denied");
        }

        MessageResponse response = visualProofService.sendVisualProof(conversationId, request.getImageUrl(), admin);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/dashboard/chat/visual-proof/{visualProofId}/accept")
    public ResponseEntity<VisualProofResponse> acceptVisualProof(@PathVariable Long visualProofId) {
        User customer = chatService.getCurrentUser();
        VisualProofResponse response = visualProofService.acceptVisualProof(visualProofId, customer);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/dashboard/chat/visual-proof/{visualProofId}/decline")
    public ResponseEntity<VisualProofResponse> declineVisualProof(@PathVariable Long visualProofId) {
        User customer = chatService.getCurrentUser();
        VisualProofResponse response = visualProofService.declineVisualProof(visualProofId, customer);
        return ResponseEntity.ok(response);
    }
}

