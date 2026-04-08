package com.example.guangzhouorder.repository;

import com.example.guangzhouorder.entity.Order;
import com.example.guangzhouorder.entity.OrderTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderTrackingRepository extends JpaRepository<OrderTracking, Long> {
    List<OrderTracking> findByOrderOrderByCreatedAtDesc(Order order);
    Optional<OrderTracking> findByOrderAndIsCurrent(Order order, Boolean isCurrent);
    List<OrderTracking> findByOrder(Order order);
}
