package com.example.guangzhouorder.repository;

import com.example.guangzhouorder.entity.Conversation;
import com.example.guangzhouorder.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByConversationOrderByCreatedAtAsc(Conversation conversation);
}
