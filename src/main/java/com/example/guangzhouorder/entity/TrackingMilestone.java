package com.example.guangzhouorder.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tracking_milestones")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackingMilestone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "milestone_id")
    private Long milestoneId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tracking_id", nullable = false)
    private OrderTracking tracking;

    @Column(name = "step_order")
    private int stepOrder;

    @Column(name = "status")
    private String status;

    @Column(name = "status_label")
    private String statusLabel;

    @Column(name = "location")
    private String location;

    @Column(name = "description")
    private String description;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @Column(name = "is_completed")
    @Builder.Default
    private boolean isCompleted = false;

    @Column(name = "is_current")
    @Builder.Default
    private boolean isCurrent = false;
}
