package com.example.guangzhouorder.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "commission_policies")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "policy_id")
    private Long policyId;

    @Column(name = "policy_name", nullable = false, unique = true)
    private String policyName;

    /**
     * Calculation method: e.g. PERCENTAGE, FLAT_RATE
     */
    @Column(name = "commission_type", nullable = false)
    private String commissionType;

    /**
     * The multiplier (e.g. 0.12 for 12%) or flat amount depending on commissionType.
     */
    @Column(name = "commission_value", nullable = false, precision = 10, scale = 4)
    private BigDecimal commissionValue;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
