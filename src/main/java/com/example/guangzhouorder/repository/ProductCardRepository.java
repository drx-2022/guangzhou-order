package com.example.guangzhouorder.repository;

import com.example.guangzhouorder.entity.ProductCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductCardRepository extends JpaRepository<ProductCard, Long> {
    List<ProductCard> findByIsPublicTrue();
    List<ProductCard> findByIsPublicTrueOrderByCreatedAtDesc();
    List<ProductCard> findTop6ByIsPublicTrue();
}
