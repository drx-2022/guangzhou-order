package com.example.guangzhouorder.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    /**
     * Stores the custom product "DNA" as a JSON string.
     * Contains materials, dimensions, technical notes, and reference details.
     */
    @Column(name = "structured_specs", columnDefinition = "TEXT")
    private String structuredSpecs;

    /**
     * Production lifecycle states:
     * DRAFT, NEGOTIATING, PENDING_DEPOSIT, IN_PRODUCTION,
     * PENDING_CUSTOMER_APPROVAL, READY_FOR_SHIPPING, DONE, CANCELLED
     */
    @Column(nullable = false)
    private String status;

    /**
     * Payment states: UNPAID, DEPOSITED, DONE
     */
    @Column(name = "payment_status")
    private String paymentStatus;

    @Column(name = "deposit_amount", precision = 18, scale = 2)
    private BigDecimal depositAmount;

    @Column(name = "final_price", precision = 18, scale = 2)
    private BigDecimal finalPrice;

    @Column(name = "visual_proof_url", columnDefinition = "TEXT")
    private String visualProofUrl;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
