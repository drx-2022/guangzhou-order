package com.example.guangzhouorder.controller;

import com.example.guangzhouorder.dto.chat.ChatMessageRequest;
import com.example.guangzhouorder.dto.chat.ConversationResponse;
import com.example.guangzhouorder.dto.chat.MessageResponse;
import com.example.guangzhouorder.entity.Conversation;
import com.example.guangzhouorder.entity.User;
import com.example.guangzhouorder.service.ChatService;
import com.example.guangzhouorder.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Validated
public class ChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final UserService userService;


    //Customer chat list: show all conversations for the customer.

    @GetMapping("/dashboard/chat")
    public String customerChatList(Authentication authentication, Model model) {
        User currentUser = chatService.getCurrentUser();
        
        // Only customers can access their own chat page
        if (!"CUSTOMER".equals(currentUser.getRole())) {
            throw new AccessDeniedException("Access denied");
        }
        
        // Load all conversations for this customer
        List<ConversationResponse> conversations = chatService.getCustomerConversations(currentUser);
        model.addAttribute("conversations", conversations);
        model.addAttribute("currentUserEmail", currentUser.getEmail());
        model.addAttribute("currentUser", currentUser);
        
        return "chat/customer_chat_list";
    }

    @GetMapping("/dashboard/chat/new")
    public String cloneOrderToChat(
            @RequestParam(required = false) Long cloneFrom,
            Authentication authentication) {

        User currentUser = chatService.getCurrentUser();
        if (!"CUSTOMER".equals(currentUser.getRole())) {
            throw new AccessDeniedException("Access denied");
        }

        Conversation conversation;
        if (cloneFrom != null) {
            conversation = chatService.createConversationWithCloneProposal(currentUser, cloneFrom);
        } else {
            conversation = chatService.createNewConversation(currentUser);
        }

        return "redirect:/dashboard/chat/" + conversation.getConversationId();
    }

    //Customer chat room: view specific conversation.

    @GetMapping("/dashboard/chat/{conversationId}")
    public String customerChatRoom(@PathVariable Long conversationId, Authentication authentication, Model model) {
        User currentUser = chatService.getCurrentUser();
        
        // Only customers can access their own chat page
        if (!"CUSTOMER".equals(currentUser.getRole())) {
            throw new AccessDeniedException("Access denied");
        }
        
        // Load conversation and verify ownership
        Conversation conversation = chatService.getConversationById(conversationId, currentUser);
        if (!conversation.getCustomer().getUserId().equals(currentUser.getUserId())) {
            throw new AccessDeniedException("Access denied");
        }
        
        // Load messages
        //List<MessageResponse> messages = chatService.getMessages(conversationId, currentUser);
        List<MessageResponse> messages = chatService.getMessagesEnriched(conversationId, currentUser);
        
        // Mark all as read
        chatService.markAllAsRead(conversationId, currentUser);
        
        // Build response
        ConversationResponse conversationResponse = ConversationResponse.from(conversation, 0);
        
        model.addAttribute("conversation", conversationResponse);
        model.addAttribute("messages", messages);
        model.addAttribute("currentUserEmail", currentUser.getEmail());
        model.addAttribute("currentUser", currentUser);
        
        return "chat/customer_chat";
    }

    //Admin chat list: show all conversations with unread counts.

    @GetMapping("/admin/chat")
    public String adminChatList(Authentication authentication, Model model) {
        User currentUser = chatService.getCurrentUser();
        
        // Only admins can access admin chat
        if (!"ADMIN".equals(currentUser.getRole())) {
            throw new AccessDeniedException("Access denied");
        }
        
        // Load all conversations with unread counts
        List<ConversationResponse> conversations = chatService.getAllConversations(currentUser);
        
        model.addAttribute("conversations", conversations);
        model.addAttribute("currentUserEmail", currentUser.getEmail());
        
        return "chat/admin_chat_list";
    }

    //Admin chat room: view specific conversation as admin.

    @GetMapping("/admin/chat/{conversationId}")
    public String adminChatRoom(@PathVariable Long conversationId, Authentication authentication, Model model) {
        User currentUser = chatService.getCurrentUser();
        
        // Only admins can access admin chat
        if (!"ADMIN".equals(currentUser.getRole())) {
            throw new AccessDeniedException("Access denied");
        }
        
        // Load conversation and messages
        Conversation conversation = chatService.getConversationById(conversationId, currentUser);
        //List<MessageResponse> messages = chatService.getMessages(conversationId, currentUser);
        List<MessageResponse> messages = chatService.getMessagesEnriched(conversationId, currentUser);
        
        // Mark all as read from admin perspective
        chatService.markAllAsRead(conversationId, currentUser);
        
        // Build response with 0 unread (just marked as read)
        ConversationResponse conversationResponse = ConversationResponse.from(conversation, 0);
        
        model.addAttribute("conversation", conversationResponse);
        model.addAttribute("messages", messages);
        model.addAttribute("currentUserEmail", currentUser.getEmail());
        
        return "chat/admin_chat_room";
    }

    //Customer creates a new conversation/request.
    //Creates a brand new conversation and redirects to it.

    @PostMapping("/dashboard/chat/create")
    public String createNewRequest(Authentication authentication) {
        User currentUser = chatService.getCurrentUser();
        
        // Only customers can create new requests
        if (!"CUSTOMER".equals(currentUser.getRole())) {
            throw new AccessDeniedException("Access denied");
        }
        
        // Create a brand new conversation
        Conversation conversation = chatService.createNewConversation(currentUser);
        
        // Redirect to the conversation with path variable
        return "redirect:/dashboard/chat/" + conversation.getConversationId();
    }

    //WebSocket endpoint: receive message from client, save it, broadcast to topic.
    //Principal is injected by Spring WebSocket authentication (from JWT via WebSocketAuthChannelInterceptor).

    @MessageMapping("/chat.send")
    public void sendMessage(@Valid @Payload ChatMessageRequest request, Principal principal) {
        // Validate request
        if (request.getConversationId() == null) {
            throw new IllegalArgumentException("conversationId cannot be null");
        }
        
        // Validate principal (authentication)
        if (principal == null) {
            throw new AccessDeniedException("WebSocket authentication required");
        }
        
        // Get sender from current authentication
        User sender = chatService.getUserByEmail(principal.getName());
        
        // Send message and get response
        MessageResponse messageResponse = chatService.sendMessage(
                request.getConversationId(),
                request.getMessageType(),
                request.getContent(),
                request.getMediaUrl(),
                sender
        );
        
        // Broadcast to the conversation topic
        simpMessagingTemplate.convertAndSend(
                "/topic/conversation." + request.getConversationId(),
                messageResponse
        );
    }
}
