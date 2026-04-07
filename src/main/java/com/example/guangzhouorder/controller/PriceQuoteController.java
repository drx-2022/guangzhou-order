package com.example.guangzhouorder.controller;

import com.example.guangzhouorder.dto.chat.PriceQuoteRequest;
import com.example.guangzhouorder.dto.chat.PriceQuoteResponse;
import com.example.guangzhouorder.entity.Conversation;
import com.example.guangzhouorder.entity.Order;
import com.example.guangzhouorder.entity.User;
import com.example.guangzhouorder.repository.ConversationRepository;
import com.example.guangzhouorder.repository.OrderRepository;
import com.example.guangzhouorder.repository.SpecProposalCardRepository;
import com.example.guangzhouorder.service.ChatService;
import com.example.guangzhouorder.service.PriceQuoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class PriceQuoteController {

    private final PriceQuoteService priceQuoteService;
    private final ChatService chatService;
    private final OrderRepository orderRepository;
    private final ConversationRepository conversationRepository;
    private final SpecProposalCardRepository specProposalCardRepository;

    @PostMapping("/admin/chat/quote/submit")
    public String adminSubmitQuote(@Valid @ModelAttribute PriceQuoteRequest request, BindingResult bindingResult, RedirectAttributes ra) {
        User admin = chatService.getCurrentUser();
        if (!"ADMIN".equals(admin.getRole())) throw new AccessDeniedException("Access denied");
        if (bindingResult.hasErrors()) {
            ra.addFlashAttribute("quoteError", bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/admin/chat/order/" + request.getOrderId();
        }
        try {
            priceQuoteService.submitQuote(request, admin);
        } catch (Exception e) {
            ra.addFlashAttribute("quoteError", e.getMessage());
        }
        return "redirect:/admin/chat/order/" + request.getOrderId();
    }

    @PostMapping("/dashboard/chat/quote/submit")
    public String customerSubmitQuote(@Valid @ModelAttribute PriceQuoteRequest request, BindingResult bindingResult, RedirectAttributes ra) {
        User customer = chatService.getCurrentUser();
        if (bindingResult.hasErrors()) {
            ra.addFlashAttribute("quoteError", bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/dashboard/chat/order/" + request.getOrderId();
        }
        try {
            priceQuoteService.submitQuote(request, customer);
        } catch (Exception e) {
            ra.addFlashAttribute("quoteError", e.getMessage());
        }
        return "redirect:/dashboard/chat/order/" + request.getOrderId();
    }

    @PostMapping("/admin/chat/quote/{quoteId}/accept")
    public String adminAcceptQuote(@PathVariable Long quoteId, RedirectAttributes ra) {
        User admin = chatService.getCurrentUser();
        try {
            PriceQuoteResponse quote = priceQuoteService.acceptQuote(quoteId, admin);
            return "redirect:/admin/chat/order/" + quote.getOrderId();
        } catch (Exception e) {
            ra.addFlashAttribute("quoteError", e.getMessage());
            return "redirect:/admin/chat";
        }
    }
    @PostMapping("/dashboard/chat/quote/{quoteId}/accept")
    public String customerAcceptQuote(@PathVariable Long quoteId, RedirectAttributes ra) {
        User customer = chatService.getCurrentUser();
        try {
            PriceQuoteResponse quote = priceQuoteService.acceptQuote(quoteId, customer);
            return "redirect:/payment/" + quote.getOrderId() + "/deposit";
        } catch (Exception e) {
            ra.addFlashAttribute("quoteError", e.getMessage());
            return "redirect:/dashboard/chat";
        }
    }
    @Transactional(readOnly = true)
    @GetMapping("/admin/chat/order/{orderId}")
    public String adminOrderView(@PathVariable Long orderId, Model model) {
        User admin = chatService.getCurrentUser();
        if (!"ADMIN".equals(admin.getRole())) throw new AccessDeniedException("Access denied");
        Order order = orderRepository.findById(orderId).orElseThrow();
        Conversation conv = conversationRepository.findByCustomerOrderByLastMessageAtDesc(order.getCustomer())
                .stream().findFirst().orElseThrow();

        List<PriceQuoteResponse> quotes = priceQuoteService.getQuotesByOrder(orderId, admin);

        java.math.BigDecimal estimatePrice = null;
        if (!quotes.isEmpty()) {
            estimatePrice = quotes.get(0).getEstimatePrice();
        } else {
            estimatePrice = specProposalCardRepository
                    .findByResultOrderId(orderId)
                    .map(card -> card.getEstimatePrice())
                    .orElse(null);
        }
        model.addAttribute("estimatePrice", estimatePrice);

        model.addAttribute("order", order);
        model.addAttribute("quotes", quotes);
        model.addAttribute("messages", chatService.getMessagesEnriched(conv.getConversationId(), admin));
        model.addAttribute("currentUserEmail", admin.getEmail());
        model.addAttribute("conversationId", conv.getConversationId());
        return "chat/admin_order_negotiation";
    }

    @Transactional(readOnly = true)
    @GetMapping("/dashboard/chat/order/{orderId}")
    public String customerOrderView(@PathVariable Long orderId, Model model) {
        User customer = chatService.getCurrentUser();
        Order order = orderRepository.findById(orderId).orElseThrow();
        if (!order.getCustomer().getUserId().equals(customer.getUserId())) throw new AccessDeniedException("Access denied");
        Conversation conv = conversationRepository.findByCustomerOrderByLastMessageAtDesc(customer)
                .stream().findFirst().orElseThrow();

        List<PriceQuoteResponse> quotes = priceQuoteService.getQuotesByOrder(orderId, customer);

        java.math.BigDecimal estimatePrice = null;
        if (!quotes.isEmpty()) {
            estimatePrice = quotes.get(0).getEstimatePrice();
        } else {
            estimatePrice = specProposalCardRepository
                    .findByResultOrderId(orderId)
                    .map(card -> card.getEstimatePrice())
                    .orElse(null);
        }
        model.addAttribute("estimatePrice", estimatePrice);

        model.addAttribute("order", order);
        model.addAttribute("quotes", quotes);
        model.addAttribute("messages", chatService.getMessagesEnriched(conv.getConversationId(), customer));
        model.addAttribute("currentUserEmail", customer.getEmail());
        model.addAttribute("conversationId", conv.getConversationId());
        return "chat/customer_order_negotiation";
    }
}
