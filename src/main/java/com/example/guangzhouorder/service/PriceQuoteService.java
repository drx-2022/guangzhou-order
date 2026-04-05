package com.example.guangzhouorder.service;

import com.example.guangzhouorder.dto.chat.MessageResponse;
import com.example.guangzhouorder.dto.chat.PriceQuoteRequest;
import com.example.guangzhouorder.dto.chat.PriceQuoteResponse;
import com.example.guangzhouorder.entity.*;
import com.example.guangzhouorder.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PriceQuoteService {

    private final PriceQuoteRepository priceQuoteRepository;
    private final OrderRepository orderRepository;
    private final SpecProposalCardRepository specProposalCardRepository;
    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final ChatService chatService;
    private final SimpMessagingTemplate simpMessagingTemplate;

    @Transactional
    public PriceQuoteResponse submitQuote(PriceQuoteRequest request, User proposer) {
        Order order = orderRepository.findById(request.getOrderId()).orElseThrow(() -> new RuntimeException("Order not found"));

        if (!"Negotiating Price".equals(order.getStatus())) {
            throw new IllegalStateException("Cannot submit quote for an order that is not in 'Negotiating Price' status.");

        }
        boolean isAdmin = "ADMIN".equals(proposer.getRole());
        boolean isCustomer = order.getCustomer().getUserId().equals(proposer.getUserId());
        if (!isAdmin && !isCustomer) {
            throw new AccessDeniedException("Access denied");
        }
        BigDecimal estimatePrice;
        PriceQuote currentPending = priceQuoteRepository.findTopByOrderAndStatusOrderByCreatedAtDesc(order, "PENDING").orElse(null);
        if (currentPending == null) {
            if (priceQuoteRepository.existsByOrder(order)) {
                throw new IllegalStateException("Quote has already been resolved.");
            }
            SpecProposalCard proposal = specProposalCardRepository
                    .findByResultOrderId(order.getOrderId())
                    .orElseThrow(() -> new IllegalStateException("No proposal card found for this order"));
            estimatePrice = proposal.getEstimatePrice();
        }

        else {
            estimatePrice = currentPending.getEstimatePrice();
            currentPending.setStatus("COUNTERED");
            priceQuoteRepository.save(currentPending);
        }
        if (request.getProposedPrice().compareTo(estimatePrice) > 0) {
            throw new IllegalArgumentException("Proposed price cannot exceed the estimate price of " + estimatePrice);
        }

        User customer = order.getCustomer();
        Conversation conversation = conversationRepository.findByCustomerOrderByLastMessageAtDesc(customer)
                .stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No conversation found for this order's customer"));

        MessageResponse msgResponse = chatService.sendMessage(
                conversation.getConversationId(),
                "PRICE_QUOTE",
                null,
                null,
                proposer);

        Message message = messageRepository.findById(msgResponse.getMessageId()).orElseThrow();

        PriceQuote quote = PriceQuote.builder()
                .order(order)
                .message(message)
                .proposedBy(proposer)
                .proposedPrice(request.getProposedPrice())
                .estimatePrice(estimatePrice)
                .note(request.getNote())
                .status("PENDING")
                .build();
        PriceQuote saved = priceQuoteRepository.save(quote);
        PriceQuoteResponse response = PriceQuoteResponse.from(saved);
        MessageResponse broadcastMsg = MessageResponse.builder()
                .messageId(msgResponse.getMessageId())
                .senderEmail(proposer.getEmail())
                .senderName(proposer.getName())
                .senderRole(proposer.getRole())
                .conversationId(conversation.getConversationId())
                .messageType("PRICE_QUOTE")
                .createdAt(msgResponse.getCreatedAt())
                .priceQuote(response)
                .build();

        simpMessagingTemplate.convertAndSend("/topic/conversation." + conversation.getConversationId(), broadcastMsg);
        return response;


    }

    public List<PriceQuoteResponse> getQuotesByOrder(Long orderId, User requestingUser) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        boolean isAdmin = "ADMIN".equals(requestingUser.getRole());
        boolean isOwner = order.getCustomer().getUserId().equals(requestingUser.getUserId());
        if (!isAdmin && !isOwner) throw new AccessDeniedException("Access denied");

        return priceQuoteRepository.findByOrderOrderByCreatedAtAsc(order)
                .stream()
                .map(PriceQuoteResponse::from)
                .collect(Collectors.toList());
    }

    public PriceQuoteResponse getQuote(Long quoteId, User requestingUser) {
        PriceQuote quote = priceQuoteRepository.findById(quoteId)
                .orElseThrow(() -> new IllegalArgumentException("Quote not found"));

        boolean isAdmin = "ADMIN".equals(requestingUser.getRole());
        boolean isOwner = quote.getOrder().getCustomer().getUserId().equals(requestingUser.getUserId());
        if (!isAdmin && !isOwner) throw new AccessDeniedException("Access denied");

        return PriceQuoteResponse.from(quote);
    }

    @Transactional
    public PriceQuoteResponse acceptQuote(Long quoteId, User acceptor) {
        PriceQuote quote = priceQuoteRepository.findById(quoteId)
                .orElseThrow(() -> new IllegalArgumentException("Quote not found"));

        if (!"PENDING".equals(quote.getStatus())) {
            throw new IllegalStateException("This quote has already been " + quote.getStatus().toLowerCase() + ".");
        }

        Order order = quote.getOrder();
        boolean isAdmin = "ADMIN".equals(acceptor.getRole());
        boolean isCustomer = order.getCustomer().getUserId().equals(acceptor.getUserId());
        if (!isAdmin && !isCustomer) {
            throw new AccessDeniedException("Access denied");
        }

        if (quote.getProposedBy().getUserId().equals(acceptor.getUserId())) {
            throw new IllegalStateException("You cannot accept your own price proposal.");
        }

        quote.setStatus("ACCEPTED");
        priceQuoteRepository.save(quote);

        order.setFinalPrice(quote.getProposedPrice());
        order.setStatus("Pending Deposit");
        order.setPaymentStatus("UNPAID");
        orderRepository.save(order);

        User customer = order.getCustomer();
        Conversation conversation = conversationRepository.findByCustomerOrderByLastMessageAtDesc(customer)
                .stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No conversation found"));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventType", "QUOTE_STATUS_UPDATE");
        payload.put("quoteId", quoteId);
        payload.put("status", "ACCEPTED");
        payload.put("finalPrice", quote.getProposedPrice());
        payload.put("orderId", order.getOrderId());

        simpMessagingTemplate.convertAndSend("/topic/conversation." + conversation.getConversationId(), (Object) payload);

        return PriceQuoteResponse.from(quote);
    }

}