package com.example.guangzhouorder.repository;

import com.example.guangzhouorder.entity.Conversation;
import com.example.guangzhouorder.entity.Message;
import com.example.guangzhouorder.entity.SpecProposalCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpecProposalCardRepository extends JpaRepository<SpecProposalCard, Long> {
    Optional<SpecProposalCard> findByMessage(Message message);
    List<SpecProposalCard> findByConversation(Conversation conversation);
    List<SpecProposalCard> findByConversationAndStatus(Conversation conversation, String status);
    boolean existsByConversationAndStatus(Conversation conversation, String status);
    Optional<SpecProposalCard> findByResultOrderId(Long resultOrderId);
}