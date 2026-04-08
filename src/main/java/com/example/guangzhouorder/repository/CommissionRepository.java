package com.example.guangzhouorder.repository;

import com.example.guangzhouorder.entity.Commission;
import com.example.guangzhouorder.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface CommissionRepository extends JpaRepository<Commission, Long> {
    List<Commission> findByAffiliate(User affiliate);
    long countByAffiliate(User affiliate);

    @Query("SELECT SUM(c.amount) FROM Commission c WHERE c.affiliate = :affiliate")
    BigDecimal sumAmountByAffiliate(@Param("affiliate") User affiliate);
}
