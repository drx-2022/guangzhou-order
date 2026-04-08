package com.example.guangzhouorder.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_trackings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tracking_id")
    private Long trackingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /**
     * Tracking status steps:
     * WAITING_FOR_PROCESSING, PROCESSING, READY_TO_SHIP,
     * IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERED
     */
    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "location", columnDefinition = "TEXT")
    private String location;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Whether this is the current/latest tracking step
     */
    @Column(name = "is_current")
    private Boolean isCurrent;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
