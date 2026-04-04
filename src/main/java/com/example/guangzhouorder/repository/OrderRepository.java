package com.example.guangzhouorder.repository;

import com.example.guangzhouorder.entity.Order;
import com.example.guangzhouorder.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomer(User customer);
    List<Order> findByCustomerOrderByUpdatedAtDesc(User customer);
    List<Order> findByCustomerAndStatus(User customer, String status);
    List<Order> findByStatus(String status);

    long countByCustomerAndStatus(User customer, String status);

    List<Order> findTop5ByCustomerAndStatusNotInOrderByUpdatedAtDesc(User customer, List<String> excludedStatuses);
}
