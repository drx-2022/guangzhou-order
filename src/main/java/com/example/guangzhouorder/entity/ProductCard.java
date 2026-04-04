package com.example.guangzhouorder.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_cards")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_card_id")
    private Long productCardId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_order_id", unique = true)
    private Order sourceOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    /**
     * Verified product "DNA": materials, dimensions, photos, technical notes.
     * Stored as a JSON string.
     */
    @Column(name = "card_dna", columnDefinition = "TEXT")
    private String cardDna;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic;

    @Column(name = "display_price", precision = 18, scale = 2)
    private BigDecimal displayPrice;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
