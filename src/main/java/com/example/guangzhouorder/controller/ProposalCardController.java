package com.example.guangzhouorder.controller;

import com.example.guangzhouorder.dto.chat.ConversationResponse;
import com.example.guangzhouorder.dto.chat.SpecProposalCardRequest;
import com.example.guangzhouorder.dto.chat.SpecProposalCardResponse;
import com.example.guangzhouorder.entity.Conversation;
import com.example.guangzhouorder.entity.User;
import com.example.guangzhouorder.service.ChatService;
import com.example.guangzhouorder.service.ProposalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class ProposalCardController {

    private final ProposalService proposalService;
    private final ChatService chatService;

    @PostMapping("/admin/chat/{conversationId}/proposal")
    public String createProposal(
            @PathVariable Long conversationId,
            @Valid @ModelAttribute SpecProposalCardRequest request,
            BindingResult bindingResult,
            Model model) {

        User admin = chatService.getCurrentUser();
        if (!"ADMIN".equals(admin.getRole())) throw new AccessDeniedException("Access denied");

        if (bindingResult.hasErrors()) {
            populateAdminChatModel(model, conversationId, admin);
            return "chat/admin_chat_room";
        }

        try {
            proposalService.createProposalCard(conversationId, request, admin);
        } catch (IllegalStateException e) {
            model.addAttribute("proposalError", e.getMessage());
            populateAdminChatModel(model, conversationId, admin);
            return "chat/admin_chat_room";
        }

        return "redirect:/admin/chat/" + conversationId;
    }

    @PostMapping("/dashboard/chat/proposal/{proposalCardId}/accept")
    public String acceptProposal(@PathVariable Long proposalCardId, RedirectAttributes redirectAttributes) {
        User customer = chatService.getCurrentUser();
        try {
            proposalService.acceptProposalCard(proposalCardId, customer);
            return "redirect:/dashboard/chat/proposal/" + proposalCardId + "/result";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/dashboard/chat";
        }
    }

    @PostMapping("/dashboard/chat/proposal/{proposalCardId}/decline")
    public String declineProposal(@PathVariable Long proposalCardId, RedirectAttributes redirectAttributes) {
        User customer = chatService.getCurrentUser();
        try {
            SpecProposalCardResponse response = proposalService.declineProposalCard(proposalCardId, customer);
            return "redirect:/dashboard/chat/" + response.getConversationId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/dashboard/chat";
        }
    }

    @GetMapping("/dashboard/chat/proposal/{proposalCardId}/result")
    public String proposalResult(@PathVariable Long proposalCardId, Model model) {
        User user = chatService.getCurrentUser();
        SpecProposalCardResponse response = proposalService.getProposalCard(proposalCardId, user);
        model.addAttribute("proposal", response);
        return "chat/proposal_result";
    }
    /**
     * Helper method to ensure the model is consistent with ChatController's requirements.
     */
    private void populateAdminChatModel(Model model, Long conversationId, User admin) {
        Conversation conversation = chatService.getConversationById(conversationId, admin);
        model.addAttribute("conversation", ConversationResponse.from(conversation, 0));
        model.addAttribute("messages", chatService.getMessagesEnriched(conversationId, admin));
        model.addAttribute("currentUserEmail", admin.getEmail());
    }
}