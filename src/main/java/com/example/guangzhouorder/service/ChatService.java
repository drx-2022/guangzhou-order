package com.example.guangzhouorder.service;

import com.example.guangzhouorder.dto.chat.ConversationResponse;
import com.example.guangzhouorder.dto.chat.MessageResponse;
import com.example.guangzhouorder.entity.Conversation;
import com.example.guangzhouorder.entity.ConversationRead;
import com.example.guangzhouorder.entity.Message;
import com.example.guangzhouorder.entity.User;
import com.example.guangzhouorder.repository.ConversationReadRepository;
import com.example.guangzhouorder.repository.ConversationRepository;
import com.example.guangzhouorder.repository.MessageRepository;
import com.example.guangzhouorder.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ConversationReadRepository conversationReadRepository;
    private final UserRepository userRepository;

    /**
     * Get or create the single conversation for a customer user.
     */
    public Conversation getOrCreateConversation(User customer) {
        List<Conversation> existing = conversationRepository.findByCustomerOrderByLastMessageAtDesc(customer);
        
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        
        // Create new conversation
        Conversation conversation = Conversation.builder()
                .customer(customer)
                .lastMessageAt(null)
                .build();
        
        return conversationRepository.save(conversation);
    }

    //Create a brand new conversation for a customer (always creates, doesn't check for existing).

    public Conversation createNewConversation(User customer) {
        Conversation conversation = Conversation.builder()
                .customer(customer)
                .lastMessageAt(null)
                .build();
        
        return conversationRepository.save(conversation);
    }

    //Get all conversations (for Admin view), include unread count for the given viewer.

    public List<ConversationResponse> getAllConversations(User viewer) {
        List<Conversation> conversations = conversationRepository.findAll();
        
        return conversations.stream()
                .map(conv -> {
                    long unreadCount = conversationReadRepository.countUnreadMessages(conv, viewer);
                    return ConversationResponse.from(conv, unreadCount);
                })
                .collect(Collectors.toList());
    }

    //Get all conversations for a customer, sorted by most recent.

    public List<ConversationResponse> getCustomerConversations(User customer) {
        List<Conversation> conversations = conversationRepository.findByCustomerOrderByLastMessageAtDesc(customer);
        
        return conversations.stream()
                .map(conv -> {
                    long unreadCount = conversationReadRepository.countUnreadMessages(conv, customer);
                    return ConversationResponse.from(conv, unreadCount);
                })
                .collect(Collectors.toList());
    }


    public Conversation getConversationById(Long conversationId, User requestingUser) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));
        
        // Check access: must be ADMIN or the customer of this conversation
        if (!"ADMIN".equals(requestingUser.getRole()) && 
            !conversation.getCustomer().getUserId().equals(requestingUser.getUserId())) {
            throw new AccessDeniedException("Access denied");
        }
        
        return conversation;
    }

    //Load all messages for a conversation as MessageResponse list.

    public List<MessageResponse> getMessages(Long conversationId, User requestingUser) {
        Conversation conversation = getConversationById(conversationId, requestingUser);
        List<Message> messages = messageRepository.findByConversationOrderByCreatedAtAsc(conversation);
        
        return messages.stream()
                .map(MessageResponse::from)
                .collect(Collectors.toList());
    }

    //Save a new message+ update conversation.lastMessageAt to now+ return the saved MessageResponse.

    @Transactional
    public MessageResponse sendMessage(Long conversationId, String messageType,
                                       String content, String mediaUrl, User sender) {
        Conversation conversation = getConversationById(conversationId, sender);
        
        Message message = Message.builder()
                .sender(sender)
                .conversation(conversation)
                .messageType(messageType)
                .content(content)
                .mediaUrl(mediaUrl)
                .build();
        
        Message saved = messageRepository.save(message);
        
        // Update conversation's lastMessageAt
        conversation.setLastMessageAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        // Auto-mark sender's message as read
        ConversationRead read = ConversationRead.builder()
                .conversation(conversation)
                .message(saved)
                .user(sender)
                .readAt(LocalDateTime.now())
                .build();
        conversationReadRepository.save(read);
        
        return MessageResponse.from(saved);
    }

    //Mark all messages in this conversation as read for this user.
    //For each Message not yet in ConversationRead for this user, insert a ConversationRead record.

    @Transactional
    public void markAllAsRead(Long conversationId, User user) {
        Conversation conversation = getConversationById(conversationId, user);
        
        // Get already-read message IDs for this user in this conversation
        List<ConversationRead> alreadyRead = conversationReadRepository.findByConversationAndUser(conversation, user);
        Set<Long> readMessageIds = alreadyRead.stream()
                .map(cr -> cr.getMessage().getMessageId())
                .collect(Collectors.toSet());
        
        // Load all messages in this conversation
        List<Message> allMessages = messageRepository.findByConversationOrderByCreatedAtAsc(conversation);
        
        // Filter out already-read messages and create ConversationRead records
        LocalDateTime now = LocalDateTime.now();
        List<ConversationRead> toInsert = allMessages.stream()
                .filter(msg -> !readMessageIds.contains(msg.getMessageId()))
                .map(msg -> ConversationRead.builder()
                        .conversation(conversation)
                        .message(msg)
                        .user(user)
                        .readAt(now)
                        .build())
                .collect(Collectors.toList());
        
        if (!toInsert.isEmpty()) {
            conversationReadRepository.saveAll(toInsert);
        }
    }

    public User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
    }
}
