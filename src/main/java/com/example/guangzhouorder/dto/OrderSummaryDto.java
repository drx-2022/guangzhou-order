package com.example.guangzhouorder.dto;

import com.example.guangzhouorder.entity.Order;
import lombok.Getter;

@Getter
public class OrderSummaryDto {

    private final Long orderId;
    private final String displayId;
    private final String name;
    private final String status;
    private final String statusLabel;
    private final int progressPercent;

    public OrderSummaryDto(Order order) {
        this.orderId = order.getOrderId();
        this.displayId = "GZ-ORD-" + order.getOrderId();
        this.name = extractName(order);
        this.status = order.getStatus();
        this.statusLabel = toLabel(order.getStatus());
        this.progressPercent = toProgress(order.getStatus());
    }

    private static String extractName(Order order) {
        String specs = order.getStructuredSpecs();
        if (specs != null && specs.contains("\"name\"")) {
            try {
                int start = specs.indexOf("\"name\"") + 7;
                // skip whitespace and colon
                while (start < specs.length() && (specs.charAt(start) == ':' || specs.charAt(start) == ' ')) start++;
                if (start < specs.length() && specs.charAt(start) == '"') {
                    int end = specs.indexOf('"', start + 1);
                    if (end > start) return specs.substring(start + 1, end);
                }
            } catch (Exception ignored) {}
        }
        return "Order #GZ-ORD-" + order.getOrderId();
    }

    private static String toLabel(String status) {
        if (status == null) return "Unknown";
        return switch (status) {
            case "DRAFT" -> "Draft";
            case "NEGOTIATING" -> "Negotiating";
            case "PENDING_DEPOSIT" -> "Pending Deposit";
            case "IN_PRODUCTION" -> "In Production";
            case "PENDING_CUSTOMER_APPROVAL" -> "Pending Approval";
            case "READY_FOR_SHIPPING" -> "Ready to Ship";
            case "DONE" -> "Completed";
            case "CANCELLED" -> "Cancelled";
            default -> status;
        };
    }

    private static int toProgress(String status) {
        if (status == null) return 0;
        return switch (status) {
            case "DRAFT" -> 5;
            case "NEGOTIATING" -> 20;
            case "PENDING_DEPOSIT" -> 40;
            case "IN_PRODUCTION" -> 65;
            case "PENDING_CUSTOMER_APPROVAL" -> 90;
            case "READY_FOR_SHIPPING", "DONE" -> 100;
            default -> 0;
        };
    }
}
