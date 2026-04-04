package com.example.guangzhouorder.controller;

import com.example.guangzhouorder.dto.DashboardStatsDto;
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

import java.util.List;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final UserService userService;
    private final OrderRepository orderRepository;

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByEmail(userDetails.getUsername());
        model.addAttribute("user", user);

        if ("AFFILIATE".equals(user.getRole())) {
            return "affiliate_dashboard";
        }

        DashboardStatsDto stats = new DashboardStatsDto(
                orderRepository.countByCustomerAndStatus(user, "NEGOTIATING"),
                orderRepository.countByCustomerAndStatus(user, "IN_PRODUCTION"),
                orderRepository.countByCustomerAndStatus(user, "PENDING_CUSTOMER_APPROVAL"),
                orderRepository.countByCustomerAndStatus(user, "DONE")
        );

        List<String> terminalStatuses = List.of("DONE", "CANCELLED");

        List<OrderSummaryDto> activeOrders = orderRepository
                .findTop5ByCustomerAndStatusNotInOrderByUpdatedAtDesc(user, terminalStatuses)
                .stream()
                .map(OrderSummaryDto::new)
                .toList();

        List<OrderSummaryDto> pendingOrders = orderRepository
                .findByCustomerAndStatus(user, "PENDING_CUSTOMER_APPROVAL")
                .stream()
                .map(OrderSummaryDto::new)
                .toList();

        model.addAttribute("stats", stats);
        model.addAttribute("activeOrders", activeOrders);
        model.addAttribute("pendingOrders", pendingOrders);

        return "customer_dashboard";
    }
}
