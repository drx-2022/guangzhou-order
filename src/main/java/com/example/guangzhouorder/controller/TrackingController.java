package com.example.guangzhouorder.controller;

import com.example.guangzhouorder.entity.Order;
import com.example.guangzhouorder.entity.OrderTracking;
import com.example.guangzhouorder.entity.User;
import com.example.guangzhouorder.repository.OrderRepository;
import com.example.guangzhouorder.repository.OrderTrackingRepository;
import com.example.guangzhouorder.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class TrackingController {

    private final OrderTrackingRepository orderTrackingRepository;
    private final OrderRepository orderRepository;
    private final UserService userService;

    @GetMapping("/orders/{orderId}/tracking")
    public String trackingDetail(@PathVariable Long orderId,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 Model model) {
        User user = userService.findByEmail(userDetails.getUsername());
        
        // Check if user is admin
        if (!"ADMIN".equals(user.getRole())) {
            return "redirect:/";
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        
        List<OrderTracking> trackingHistory = orderTrackingRepository.findByOrderOrderByCreatedAtDesc(order);
        
        model.addAttribute("order", order);
        model.addAttribute("trackingHistory", trackingHistory);
        model.addAttribute("user", user);
        
        return "admin/order_tracking";
    }

    @PostMapping("/api/tracking/update/{orderId}")
    @ResponseBody
    public ResponseEntity<?> updateTracking(@PathVariable Long orderId,
                                             @RequestBody Map<String, String> request,
                                             @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername());
        
        // Check if user is admin
        if (!"ADMIN".equals(user.getRole())) {
            return ResponseEntity.status(403).body(Map.of("error", "Unauthorized"));
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        String status = request.get("status");
        String location = request.get("location");
        String description = request.get("description");

        // Set previous tracking as not current
        orderTrackingRepository.findByOrderAndIsCurrent(order, true)
                .ifPresent(tracking -> {
                    tracking.setIsCurrent(false);
                    orderTrackingRepository.save(tracking);
                });

        // Create new tracking
        OrderTracking tracking = OrderTracking.builder()
                .order(order)
                .status(status)
                .location(location)
                .description(description)
                .isCurrent(true)
                .build();

        orderTrackingRepository.save(tracking);

        return ResponseEntity.ok(Map.of("success", true, "message", "Tracking updated successfully"));
    }

    @GetMapping("/api/tracking/{orderId}")
    @ResponseBody
    public ResponseEntity<?> getTracking(@PathVariable Long orderId,
                                          @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername());
        
        // Check if user is admin
        if (!"ADMIN".equals(user.getRole())) {
            return ResponseEntity.status(403).body(Map.of("error", "Unauthorized"));
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        List<OrderTracking> trackingHistory = orderTrackingRepository.findByOrderOrderByCreatedAtDesc(order);

        return ResponseEntity.ok(trackingHistory);
    }

    @GetMapping("/api/tracking/{orderId}/current")
    @ResponseBody
    public ResponseEntity<?> getCurrentTracking(@PathVariable Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        return orderTrackingRepository.findByOrderAndIsCurrent(order, true)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
