package com.example.guangzhouorder.repository;

import com.example.guangzhouorder.entity.Conversation;
import com.example.guangzhouorder.entity.ConversationRead;
import com.example.guangzhouorder.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationReadRepository extends JpaRepository<ConversationRead, Long> {
    List<ConversationRead> findByConversationAndUser(Conversation conversation, User user);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversation = :conversation " +
           "AND m.messageId NOT IN (SELECT cr.message.messageId FROM ConversationRead cr WHERE cr.user = :user AND cr.conversation = :conversation)")
    long countUnreadMessages(@Param("conversation") Conversation conversation, @Param("user") User user);
}
