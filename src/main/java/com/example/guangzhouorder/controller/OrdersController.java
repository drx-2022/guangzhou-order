package com.example.guangzhouorder.controller;

import com.example.guangzhouorder.dto.OrderSummaryDto;
import com.example.guangzhouorder.entity.Order;
import com.example.guangzhouorder.entity.User;
import com.example.guangzhouorder.repository.OrderRepository;
import com.example.guangzhouorder.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class OrdersController {

    private final UserService userService;
    private final OrderRepository orderRepository;

    @GetMapping("/orders")
    public String myOrders(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByEmail(userDetails.getUsername());
        List<OrderSummaryDto> orders = orderRepository.findByCustomerOrderByUpdatedAtDesc(user)
                .stream()
                .map(OrderSummaryDto::new)
                .toList();

        // Calculate dashboard statistics
        long totalOrders = orders.size();
        long processingOrders = orders.stream()
                .filter(o -> "IN_PRODUCTION".equals(o.getStatus()) || "PENDING_CUSTOMER_APPROVAL".equals(o.getStatus()))
                .count();
        long negotiatingOrders = orders.stream()
                .filter(o -> "NEGOTIATING".equals(o.getStatus()))
                .count();
        long completedOrders = orders.stream()
                .filter(o -> "DONE".equals(o.getStatus()))
                .count();

        model.addAttribute("user", user);
        model.addAttribute("orders", orders);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("processingOrders", processingOrders);
        model.addAttribute("negotiatingOrders", negotiatingOrders);
        model.addAttribute("completedOrders", completedOrders);
        return "customer/my_orders";
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable Long id,
                              @AuthenticationPrincipal UserDetails userDetails,
                              Model model) {
        User user = userService.findByEmail(userDetails.getUsername());
        Order order = orderRepository.findById(id)
                .filter(o -> o.getCustomer().getUserId().equals(user.getUserId()))
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        model.addAttribute("user", user);
        model.addAttribute("order", new OrderSummaryDto(order));
        return "my_orders";
    }

    @GetMapping("/orders/{id}/review")
    public String reviewVisualProof(@PathVariable Long id,
                                    @AuthenticationPrincipal UserDetails userDetails,
                                    Model model) {
        User user = userService.findByEmail(userDetails.getUsername());
        Order order = orderRepository.findById(id)
                .filter(o -> o.getCustomer().getUserId().equals(user.getUserId()))
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        model.addAttribute("user", user);
        model.addAttribute("order", new OrderSummaryDto(order));
        model.addAttribute("visualProofUrl", order.getVisualProofUrl());
        return "visual_proof_review";
    }
}