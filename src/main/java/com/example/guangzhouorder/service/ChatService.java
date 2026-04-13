package com.example.guangzhouorder.service;

import com.example.guangzhouorder.dto.chat.SpecProposalCardRequest;
import com.example.guangzhouorder.dto.chat.ConversationResponse;
import com.example.guangzhouorder.dto.chat.MessageResponse;
import com.example.guangzhouorder.dto.chat.PriceQuoteResponse;
import com.example.guangzhouorder.dto.chat.SpecProposalCardResponse;
import com.example.guangzhouorder.dto.chat.VisualProofResponse;
import com.example.guangzhouorder.entity.Conversation;
import com.example.guangzhouorder.entity.ConversationRead;
import com.example.guangzhouorder.entity.Message;
import com.example.guangzhouorder.entity.ProductCard;
import com.example.guangzhouorder.entity.User;
import com.example.guangzhouorder.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ConversationReadRepository conversationReadRepository;
    private final UserRepository userRepository;
    private final ProductCardRepository productCardRepository;
    private final SpecProposalCardRepository specProposalCardRepository;
    private final PriceQuoteRepository priceQuoteRepository;
    private final VisualProofRepository visualProofRepository;
    private final ObjectMapper objectMapper;
    private final ProposalService proposalService;

    @Autowired
    public ChatService(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            ConversationReadRepository conversationReadRepository,
            UserRepository userRepository,
            ProductCardRepository productCardRepository,
            SpecProposalCardRepository specProposalCardRepository,
            VisualProofRepository visualProofRepository,
            PriceQuoteRepository priceQuoteRepository,
            ObjectMapper objectMapper,
            @Lazy ProposalService proposalService
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.conversationReadRepository = conversationReadRepository;
        this.userRepository = userRepository;
        this.productCardRepository = productCardRepository;
        this.specProposalCardRepository = specProposalCardRepository;
        this.visualProofRepository = visualProofRepository;
        this.priceQuoteRepository = priceQuoteRepository;
        this.objectMapper = objectMapper;
        this.proposalService = proposalService;
    }

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

    @Transactional
    public Conversation createConversationFromClone(User customer, Long productCardId) {
        Conversation conversation = createNewConversation(customer);

        ProductCard card = productCardRepository.findById(productCardId)
                .filter(ProductCard::isPublic)
                .orElse(null);

        if (card == null) {
            return conversation;
        }

        String categoryName = card.getCategory() != null ? card.getCategory().getName() : "N/A";
        String referencePrice = card.getDisplayPrice() != null
                ? card.getDisplayPrice().toPlainString() + " ₫"
                : "Contact for price";

        StringBuilder content = new StringBuilder();
        content.append("🔁 Clone Request\n\n")
                .append("Product: ").append(card.getCardName()).append("\n")
                .append("SKU: GZ-").append(card.getProductCardId()).append("\n")
                .append("Category: ").append(categoryName).append("\n")
                .append("Reference Price: ").append(referencePrice);

        if (card.getCardDna() != null && !card.getCardDna().isBlank()) {
            content.append("\n\nDNA Specs:\n").append(card.getCardDna());
        }

        sendMessage(
                conversation.getConversationId(),
                "TEXT",
                content.toString(),
                null,
                customer
        );

        return conversation;
    }

    @Transactional
    public Conversation createConversationWithCloneProposal(User customer, Long productCardId) {
        Conversation conv = createNewConversation(customer);

        ProductCard card = productCardRepository.findById(productCardId)
                .filter(ProductCard::isPublic)
                .orElse(null);
        if (card == null) {
            return conv;
        }

        Optional<User> adminOpt = userRepository.findFirstByRole("ADMIN");
        if (adminOpt.isEmpty()) {
            return conv;
        }

        String material = "";
        String configuration = "";
        String imageUrl = "";
        String notes = "";

        try {
            String dna = card.getCardDna();
            if (dna != null && !dna.isBlank()) {
                JsonNode root = objectMapper.readTree(dna);
                material = textOrEmpty(root.get("material"));
                configuration = textOrEmpty(root.get("configuration"));
                imageUrl = textOrEmpty(root.get("imageUrl"));
                notes = textOrEmpty(root.get("notes"));
            }
        } catch (Exception e) {
            log.warn("Failed parsing ProductCard DNA for clone productCardId={}", productCardId, e);
        }

        String cardName = firstNonBlank(card.getCardName(), "Unnamed Product");

        SpecProposalCardRequest request = SpecProposalCardRequest.builder()
                .materials(firstNonBlank(material, cardName, "To be specified"))
                .dimensions(firstNonBlank(configuration, "To be specified"))
                .color("To be specified")
                .quantity(1)
                .estimatePrice(card.getDisplayPrice() != null ? card.getDisplayPrice() : new BigDecimal("1.00"))
                .technicalNotes("Cloned from: " + cardName + ". " + notes)
                .referencePhotoUrls(!imageUrl.isBlank() ? List.of(imageUrl) : Collections.emptyList())
                .build();

        try {
            proposalService.createProposalCard(conv.getConversationId(), request, adminOpt.get());
        } catch (Exception e) {
            log.error("Failed auto-creating clone proposal for conversationId={} productCardId={}",
                    conv.getConversationId(), productCardId, e);
        }

        return conv;
    }

    private String textOrEmpty(JsonNode node) {
        if (node == null || node.isNull()) {
            return "";
        }
        String value = node.asText();
        return value == null ? "" : value.trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
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

    public List<MessageResponse> getMessagesEnriched(Long conversationId, User requestingUser) {
        Conversation conversation = getConversationById(conversationId, requestingUser);
        List<Message> messages = messageRepository.findByConversationOrderByCreatedAtAsc(conversation);

        return messages.stream().map(msg -> {
            MessageResponse response = MessageResponse.from(msg);
            if ("SPEC_PROPOSAL_CARD".equals(msg.getMessageType())) {
                specProposalCardRepository.findByMessage(msg).ifPresent(card ->
                        response.setProposalCard(SpecProposalCardResponse.from(card))
                );
            }
            if ("PRICE_QUOTE".equals(msg.getMessageType())) {
                priceQuoteRepository.findByMessage(msg).ifPresent(quote ->
                        response.setPriceQuote(PriceQuoteResponse.from(quote))
                );
            }
            if ("VISUAL_PROOF".equals(msg.getMessageType())) {
                visualProofRepository.findByMessage(msg).ifPresent(proof ->
                        response.setVisualProof(VisualProofResponse.from(proof))
                );
            }
            return response;
        }).collect(Collectors.toList());
    }
}
