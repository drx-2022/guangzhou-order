package com.example.guangzhouorder.service;

import com.example.guangzhouorder.entity.Order;
import com.example.guangzhouorder.entity.Payment;
import com.example.guangzhouorder.entity.ProductCard;
import com.example.guangzhouorder.entity.User;
import com.example.guangzhouorder.repository.OrderRepository;
import com.example.guangzhouorder.repository.PaymentRepository;
import com.example.guangzhouorder.repository.ProductCardRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final ProductCardRepository productCardRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${payos.client-id:}")
    private String clientId;

    @Value("${payos.api-key:}")
    private String apiKey;

    @Value("${payos.checksum-key:}")
    private String checksumKey;

    @Value("${app.frontend.base-url:http://localhost:8080}")
    private String baseUrl;

    private static final String PAYOS_API_URL = "https://api-merchant.payos.vn/v2/payment-requests";

    public Payment createDepositPayment(Long orderId, User user) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getCustomer().getUserId().equals(user.getUserId())) {
            throw new AccessDeniedException("Access denied");
        }

        if (!"Pending Deposit".equals(order.getStatus())) {
            throw new IllegalStateException("Order is not in Pending Deposit status");
        }

        paymentRepository.findFirstByOrderAndPaymentTypeOrderByPaymentIdDesc(order, "DEPOSIT")
                .ifPresent(existing -> {
                    if ("COMPLETED".equals(existing.getStatus())) {
                        throw new IllegalStateException("Deposit already paid");
                    }
                });

        BigDecimal depositAmount = order.getFinalPrice()
                .multiply(new BigDecimal("0.3"))
                .setScale(0, RoundingMode.HALF_UP);

        order.setDepositAmount(depositAmount);
        orderRepository.save(order);

        return createPayOSPayment(order, depositAmount, "DEPOSIT");
    }

    public Payment createBalancePayment(Long orderId, User user) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getCustomer().getUserId().equals(user.getUserId())) {
            throw new AccessDeniedException("Access denied");
        }

        if (!"DEPOSITED".equals(order.getPaymentStatus())) {
            throw new IllegalStateException("Order deposit not yet completed");
        }

        paymentRepository.findFirstByOrderAndPaymentTypeOrderByPaymentIdDesc(order, "BALANCE")
                .ifPresent(existing -> {
                    if ("COMPLETED".equals(existing.getStatus())) {
                        throw new IllegalStateException("Balance already paid");
                    }
                });

        BigDecimal balanceAmount = order.getFinalPrice().subtract(order.getDepositAmount());

        return createPayOSPayment(order, balanceAmount, "BALANCE");
    }

    private Payment createPayOSPayment(Order order, BigDecimal amount, String paymentType) {
        long orderCode = generateOrderCode(order.getOrderId());
        int amountInVND = amount.intValue();
        String description = paymentType.equals("DEPOSIT") ? "Coc DH " + order.getOrderId() : "TT DH " + order.getOrderId();

        String cancelUrl = baseUrl + "/payment/" + order.getOrderId() + "/cancel?type=" + paymentType;
        String returnUrl = baseUrl + "/payment/" + order.getOrderId() + "/return?type=" + paymentType;

        String rawData = String.format(
                "amount=%d&cancelUrl=%s&description=%s&orderCode=%d&returnUrl=%s",
                amountInVND, cancelUrl, description, orderCode, returnUrl
        );

        String signature = hmacSha256(rawData, checksumKey);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("orderCode", orderCode);
        body.put("amount", amountInVND);
        body.put("description", description);
        body.put("cancelUrl", cancelUrl);
        body.put("returnUrl", returnUrl);

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", paymentType.equals("DEPOSIT") ? "Deposit" : "Balance");
        item.put("quantity", 1);
        item.put("price", amountInVND);
        body.put("items", List.of(item));

        body.put("signature", signature);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-client-id", clientId);
        headers.set("x-api-key", apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                PAYOS_API_URL, entity, String.class
        );

        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            String code = root.get("code").asText();
            if (!"00".equals(code)) {
                String desc = root.has("desc") ? root.get("desc").asText() : "Unknown error";
                throw new RuntimeException("PayOS API error: " + desc);
            }

            JsonNode data = root.get("data");
            String checkoutUrl = data.get("checkoutUrl").asText();
            String qrCode = data.has("qrCode") && !data.get("qrCode").isNull() ? data.get("qrCode").asText() : null;
            String paymentLinkId = data.get("paymentLinkId").asText();

            Payment payment = Payment.builder()
                    .order(order)
                    .amount(amount)
                    .paymentType(paymentType)
                    .status("PENDING")
                    .payosOrderCode(orderCode)
                    .payosPaymentLinkId(paymentLinkId)
                    .payosCheckoutUrl(checkoutUrl)
                    .payosQrCode(qrCode)
                    .build();

            return paymentRepository.save(payment);
        } catch (Exception e) {
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new RuntimeException("Failed to parse PayOS response: " + response.getBody(), e);
        }
    }

    @Transactional
    public String handleWebhook(String requestBody) {
        try {
            JsonNode root = objectMapper.readTree(requestBody);
            String code = root.get("code").asText();
            boolean success = root.has("success") && root.get("success").asBoolean();

            if (!"00".equals(code) || !success) {
                log.warn("Webhook not success: code={}", code);
                return "IGNORED";
            }

            JsonNode data = root.get("data");
            Long orderCode = data.get("orderCode").asLong();
            String status = data.get("status").asText();

            Payment payment = paymentRepository.findByPayosOrderCode(orderCode)
                    .orElseThrow(() -> new RuntimeException("Payment not found for orderCode: " + orderCode));

            if ("PAID".equals(status)) {
                payment.setStatus("COMPLETED");
                paymentRepository.save(payment);

                Order order = payment.getOrder();
                if ("DEPOSIT".equals(payment.getPaymentType())) {
                    order.setPaymentStatus("DEPOSITED");
                    order.setStatus("IN_PRODUCTION");
                } else if ("BALANCE".equals(payment.getPaymentType())) {
                    order.setPaymentStatus("DONE");
                    
                    // Create ProductCard upon full payment if it doesn't exist
                    if (productCardRepository.findBySourceOrder(order).isEmpty()) {
                        ProductCard productCard = ProductCard.builder()
                                .cardName("Product Card for Order " + order.getOrderId())
                                .cardDna(order.getStructuredSpecs())
                                .displayPrice(order.getFinalPrice())
                                .isPublic(false)
                                .sourceOrder(order)
                                .build();
                        productCardRepository.save(productCard);
                        log.info("Created ProductCard for fully paid Order ID: {}", order.getOrderId());
                    }
                }
                orderRepository.save(order);

                log.info("Payment COMPLETED: orderCode={}, type={}", orderCode, payment.getPaymentType());
            } else if ("CANCELLED".equals(status)) {
                payment.setStatus("CANCELLED");
                paymentRepository.save(payment);
                log.info("Payment CANCELLED: orderCode={}", orderCode);
            }

            return "SUCCESS";
        } catch (Exception e) {
            log.error("Failed to handle webhook: {}", e.getMessage(), e);
            return "ERROR: " + e.getMessage();
        }
    }

    private long generateOrderCode(Long orderId) {
        long min = 1_000_000;
        long max = 9_999_999;
        return min + (long) (Math.random() * (max - min + 1));
    }

    private String hmacSha256(String data, String key) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(hmac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate HMAC SHA256", e);
        }
    }

    public Payment getPaymentByOrderAndType(Long orderId, String paymentType, User user) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getCustomer().getUserId().equals(user.getUserId())) {
            throw new AccessDeniedException("Access denied");
        }

        return paymentRepository.findByOrderAndPaymentType(order, paymentType)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
    }

    public List<Payment> getPaymentsByOrder(Long orderId, User user) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getCustomer().getUserId().equals(user.getUserId())) {
            throw new AccessDeniedException("Access denied");
        }

        return paymentRepository.findByOrderOrderByCreatedAtAsc(order);
    }
}
