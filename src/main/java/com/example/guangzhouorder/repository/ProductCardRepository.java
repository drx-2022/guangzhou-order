package com.example.guangzhouorder.repository;

import com.example.guangzhouorder.entity.Category;
import com.example.guangzhouorder.entity.Order;
import com.example.guangzhouorder.entity.ProductCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductCardRepository extends JpaRepository<ProductCard, Long> {
    List<ProductCard> findByIsPublicTrue();
    List<ProductCard> findByIsPublicTrueOrderByCreatedAtDesc();
    List<ProductCard> findByIsPublicTrueAndCategoryOrderByCreatedAtDesc(Category category);
    List<ProductCard> findTop6ByIsPublicTrue();
    Optional<ProductCard> findBySourceOrder(Order order);
}
