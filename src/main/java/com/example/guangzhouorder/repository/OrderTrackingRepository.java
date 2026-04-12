package com.example.guangzhouorder.repository;

import com.example.guangzhouorder.entity.OrderTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderTrackingRepository extends JpaRepository<OrderTracking, Long> {
    Optional<OrderTracking> findByOrderOrderId(Long orderId);
    Optional<OrderTracking> findByTrackingNumber(String trackingNumber);
    boolean existsByOrderOrderId(Long orderId);
}
