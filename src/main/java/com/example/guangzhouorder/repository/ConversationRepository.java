package com.example.guangzhouorder.repository;

import com.example.guangzhouorder.entity.Conversation;
import com.example.guangzhouorder.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    List<Conversation> findByCustomerOrderByLastMessageAtDesc(User customer);
}
