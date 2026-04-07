package com.example.guangzhouorder.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal amount;

    /**
     * DEPOSIT or BALANCE
     */
    @Column(name = "payment_type", nullable = false)
    private String paymentType;

    /**
     * PENDING, COMPLETED, FAILED, CANCELLED
     */
    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "payos_order_code")
    private Long payosOrderCode;

    @Column(name = "payos_payment_link_id")
    private String payosPaymentLinkId;

    @Column(name = "payos_checkout_url")
    private String payosCheckoutUrl;

    @Column(name = "payos_qr_code")
    private String payosQrCode;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
