package com.example.guangzhouorder.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "spec_proposal_cards")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpecProposalCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "proposal_card_id")
    private Long proposalCardId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false, unique = true)
    private Message message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Column(name = "card_dna", columnDefinition = "TEXT", nullable = false)
    private String cardDna;

    @Column(name = "estimate_price", precision = 18, scale = 2)
    private BigDecimal estimatePrice;

    @Column(name = "status", nullable = false)
    private String status;  // "PENDING", "ACCEPTED", "DECLINED"

    @Column(name = "result_order_id")
    private Long resultOrderId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

