package com.example.guangzhouorder.repository;

import com.example.guangzhouorder.entity.Order;
import com.example.guangzhouorder.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByOrderOrderByCreatedAtAsc(Order order);
    Optional<Payment> findByPayosOrderCode(Long payosOrderCode);
    Optional<Payment> findByOrderAndPaymentType(Order order, String paymentType);
    Optional<Payment> findFirstByOrderAndPaymentTypeOrderByPaymentIdDesc(Order order, String paymentType);
}
