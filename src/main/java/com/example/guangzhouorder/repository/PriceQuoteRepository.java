package com.example.guangzhouorder.repository;

import com.example.guangzhouorder.entity.Message;
import com.example.guangzhouorder.entity.Order;
import com.example.guangzhouorder.entity.PriceQuote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PriceQuoteRepository extends JpaRepository<PriceQuote, Long> {

    List<PriceQuote> findByOrderOrderByCreatedAtAsc(Order order);

    Optional<PriceQuote> findTopByOrderAndStatusOrderByCreatedAtDesc(Order order, String status);

    boolean existsByOrder(Order order);

    Optional<PriceQuote> findByMessage(Message message);
}