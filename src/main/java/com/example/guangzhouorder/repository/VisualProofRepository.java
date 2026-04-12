package com.example.guangzhouorder.repository;

import com.example.guangzhouorder.entity.Message;
import com.example.guangzhouorder.entity.VisualProof;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VisualProofRepository extends JpaRepository<VisualProof, Long> {
    Optional<VisualProof> findByMessage(Message message);
}

