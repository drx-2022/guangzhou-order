package com.example.guangzhouorder.controller;

import com.example.guangzhouorder.entity.Order;
import com.example.guangzhouorder.entity.Payment;
import com.example.guangzhouorder.entity.User;
import com.example.guangzhouorder.repository.OrderRepository;
import com.example.guangzhouorder.service.PaymentService;
import com.example.guangzhouorder.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderRepository orderRepository;
    private final UserService userService;

    public PaymentController(PaymentService paymentService,
                             OrderRepository orderRepository,
                             UserService userService) {
        this.paymentService = paymentService;
        this.orderRepository = orderRepository;
        this.userService = userService;
    }

    @GetMapping("/payment/{orderId}/deposit")
    public String showDepositPage(@PathVariable Long orderId,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  @RequestParam(required = false) String error,
                                  Model model) {
        try {
            User user = userService.findByEmail(userDetails.getUsername());
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            if (!order.getCustomer().getUserId().equals(user.getUserId())) {
                throw new IllegalArgumentException("Access denied");
            }

            Payment payment = paymentService.createDepositPayment(orderId, user);

            model.addAttribute("user", user);
            model.addAttribute("order", order);
            model.addAttribute("payment", payment);
            model.addAttribute("paymentType", "DEPOSIT");
            model.addAttribute("title", "Thanh toan tien coc (30%)");
            model.addAttribute("amount", payment.getAmount());
            model.addAttribute("finalPrice", order.getFinalPrice());
            model.addAttribute("depositAmount", payment.getAmount());
            model.addAttribute("balanceAmount", order.getFinalPrice().subtract(payment.getAmount()));
            model.addAttribute("error", error);

            return "payment_page";
        } catch (IllegalStateException e) {
            return "redirect:/payment/" + orderId + "/deposit?error=" + encodeError(e.getMessage());
        } catch (AccessDeniedException e) {
            return "redirect:/orders?error=access_denied";
        }
    }

    @GetMapping("/payment/{orderId}/balance")
    public String showBalancePage(@PathVariable Long orderId,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  @RequestParam(required = false) String error,
                                  Model model) {
        try {
            User user = userService.findByEmail(userDetails.getUsername());
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            if (!order.getCustomer().getUserId().equals(user.getUserId())) {
                throw new IllegalArgumentException("Access denied");
            }

            Payment payment = paymentService.createBalancePayment(orderId, user);

            model.addAttribute("user", user);
            model.addAttribute("order", order);
            model.addAttribute("payment", payment);
            model.addAttribute("paymentType", "BALANCE");
            model.addAttribute("title", "Thanh toan so tien con lai (70%)");
            model.addAttribute("amount", payment.getAmount());
            model.addAttribute("finalPrice", order.getFinalPrice());
            model.addAttribute("depositAmount", order.getDepositAmount());
            model.addAttribute("balanceAmount", payment.getAmount());
            model.addAttribute("error", error);

            return "payment_page";
        } catch (IllegalStateException e) {
            return "redirect:/payment/" + orderId + "/balance?error=" + encodeError(e.getMessage());
        } catch (AccessDeniedException e) {
            return "redirect:/orders?error=access_denied";
        }
    }

    @GetMapping("/payment/{orderId}/return")
    public String paymentReturn(@PathVariable Long orderId,
                                @RequestParam String type,
                                @AuthenticationPrincipal UserDetails userDetails,
                                Model model) {
        User user = userService.findByEmail(userDetails.getUsername());
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getCustomer().getUserId().equals(user.getUserId())) {
            throw new IllegalArgumentException("Access denied");
        }

        if ("DEPOSIT".equals(type)) {
            if ("DEPOSITED".equals(order.getPaymentStatus())) {
                model.addAttribute("success", true);
                model.addAttribute("message", "Thanh toan tien coc thanh cong!");
            } else {
                return "redirect:/payment/" + orderId + "/deposit?error=payment_not_confirmed";
            }
        } else if ("BALANCE".equals(type)) {
            if ("DONE".equals(order.getPaymentStatus())) {
                model.addAttribute("success", true);
                model.addAttribute("message", "Thanh toan cuoi cung thanh cong!");
            } else {
                return "redirect:/payment/" + orderId + "/balance?error=payment_not_confirmed";
            }
        }

        model.addAttribute("order", order);
        model.addAttribute("user", user);
        return "payment_return";
    }

    @GetMapping("/payment/{orderId}/cancel")
    public String paymentCancel(@PathVariable Long orderId,
                                @RequestParam String type,
                                @AuthenticationPrincipal UserDetails userDetails) {
        if ("DEPOSIT".equals(type)) {
            return "redirect:/payment/" + orderId + "/deposit?error=payment_cancelled";
        } else {
            return "redirect:/payment/" + orderId + "/balance?error=payment_cancelled";
        }
    }

    @PostMapping("/payment/webhook")
    @ResponseBody
    public String handleWebhook(HttpServletRequest request) {
        try {
            String body = request.getReader().lines().reduce("", String::concat);
            String result = paymentService.handleWebhook(body);
            if ("SUCCESS".equals(result)) {
                return "{\"code\":\"00\",\"desc\":\"success\"}";
            }
            return "{\"code\":\"01\",\"desc\":\"" + result + "\"}";
        } catch (Exception e) {
            return "{\"code\":\"99\",\"desc\":\"" + e.getMessage() + "\"}";
        }
    }

    @GetMapping("/payment/{orderId}/status")
    @ResponseBody
    public String checkPaymentStatus(@PathVariable Long orderId,
                                     @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername());
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getCustomer().getUserId().equals(user.getUserId())) {
            return "{\"error\":\"Access denied\"}";
        }

        return "{\"paymentStatus\":\"" + order.getPaymentStatus() + "\",\"orderStatus\":\"" + order.getStatus() + "\"}";
    }

    @ExceptionHandler(IllegalStateException.class)
    public String handleIllegalStateException(IllegalStateException e, Model model) {
        model.addAttribute("error", true);
        model.addAttribute("errorMessage", e.getMessage());
        return "payment_error";
    }

    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDeniedException(AccessDeniedException e, Model model) {
        model.addAttribute("error", true);
        model.addAttribute("errorMessage", "Ban khong co quyen truy cap don hang nay");
        return "payment_error";
    }

    @ExceptionHandler(RuntimeException.class)
    public String handleRuntimeException(RuntimeException e, Model model) {
        model.addAttribute("error", true);
        model.addAttribute("errorMessage", "Co loi xay ra. Vui long thu lai sau.");
        return "payment_error";
    }

    private String encodeError(String message) {
        // Encode common error messages to short codes
        if (message.contains("not in Pending Deposit status")) return "invalid_order_status";
        if (message.contains("Deposit already paid")) return "deposit_already_paid";
        if (message.contains("deposit not yet completed")) return "deposit_not_completed";
        if (message.contains("Balance already paid")) return "balance_already_paid";
        return "error";
    }
}
