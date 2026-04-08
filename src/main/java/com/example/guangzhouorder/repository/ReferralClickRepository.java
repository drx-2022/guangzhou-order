package com.example.guangzhouorder.repository;

import com.example.guangzhouorder.entity.ReferralClick;
import com.example.guangzhouorder.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReferralClickRepository extends JpaRepository<ReferralClick, Long> {
    long countByAffiliate(User affiliate);
}
