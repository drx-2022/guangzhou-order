package com.example.guangzhouorder.repository;

import com.example.guangzhouorder.entity.CommissionPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommissionPolicyRepository extends JpaRepository<CommissionPolicy, Long> {
    List<CommissionPolicy> findByIsActiveTrue();
}
