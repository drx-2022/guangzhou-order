package com.example.guangzhouorder.controller;

import com.example.guangzhouorder.dto.AffiliateDashboardStatsDto;
import com.example.guangzhouorder.dto.AffiliateReferralDto;
import com.example.guangzhouorder.dto.DashboardStatsDto;
import com.example.guangzhouorder.dto.OrderSummaryDto;
import com.example.guangzhouorder.entity.Order;
import com.example.guangzhouorder.entity.User;
import com.example.guangzhouorder.repository.CommissionRepository;
import com.example.guangzhouorder.repository.OrderRepository;
import com.example.guangzhouorder.repository.ProductCardRepository;
import com.example.guangzhouorder.repository.ReferralClickRepository;
import com.example.guangzhouorder.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final UserService userService;
    private final OrderRepository orderRepository;
    private final CommissionRepository commissionRepository;
    private final ProductCardRepository productCardRepository;
    private final ReferralClickRepository referralClickRepository;
    private final ObjectMapper objectMapper;

    @GetMapping({"customer/dashboard", "affiliate/dashboard"})
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails,
                            HttpServletRequest request,
                            Model model) {
        User user = userService.findByEmail(userDetails.getUsername());
        model.addAttribute("user", user);
        model.addAttribute("currentUser", user); // For settings modal

        if ("AFFILIATE".equals(user.getRole())) {
            BigDecimal totalCommission = commissionRepository.sumAmountByAffiliate(user);
            if (totalCommission == null) {
                totalCommission = BigDecimal.ZERO;
            }

            long totalClicks = referralClickRepository.countByAffiliate(user);
            long totalConversions = commissionRepository.countByAffiliate(user);
            double conversionRate = totalClicks > 0
                    ? ((double) totalConversions * 100.0) / totalClicks
                    : 0.0;

            AffiliateDashboardStatsDto stats = new AffiliateDashboardStatsDto(
                    totalClicks,
                    conversionRate,
                    totalCommission,
                    BigDecimal.ZERO
            );

            model.addAttribute("stats", stats);
            model.addAttribute("currentUser", user);
            model.addAttribute("referrals", buildAffiliateReferrals(user, request));
            return "affiliate/affiliate_dashboard";
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

        return "customer/customer_dashboard";
    }

    @GetMapping("/affiliate/catalog")
    public String affiliateCatalog(@AuthenticationPrincipal UserDetails userDetails,
                                   HttpServletRequest request,
                                   Model model) {
        User user = userService.findByEmail(userDetails.getUsername());

        if (!"AFFILIATE".equals(user.getRole())) {
            throw new AccessDeniedException("Access denied");
        }

        List<AffiliateReferralDto> referrals = buildAffiliateReferrals(user, request);

        model.addAttribute("referrals", referrals);
        model.addAttribute("currentUser", user);
        model.addAttribute("user", user);

        return "affiliate/affiliate_catalog";
    }

    private List<AffiliateReferralDto> buildAffiliateReferrals(User user, HttpServletRequest request) {
        String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();

        return productCardRepository.findByIsPublicTrueOrderByCreatedAtDesc()
                .stream()
                .map(card -> {
                    String imageUrl = "";
                    try {
                        if (card.getCardDna() != null && !card.getCardDna().isBlank()) {
                            imageUrl = objectMapper.readTree(card.getCardDna()).path("imageUrl").asText("");
                        }
                    } catch (Exception ignored) {
                        imageUrl = "";
                    }

                    String cardName = card.getCardName();
                    if (cardName == null || cardName.isBlank()) {
                        cardName = "Unnamed Product";
                    }

                    return new AffiliateReferralDto(
                            card.getProductCardId(),
                            "GZ-" + card.getProductCardId(),
                            cardName,
                            imageUrl,
                            card.getCategory() != null ? card.getCategory().getName() : "—",
                            "Commission",
                            card.getDisplayPrice(),
                            baseUrl + "/products/" + card.getProductCardId() + "?ref=" + user.getUserId()
                    );
                })
                .toList();
    }
}
