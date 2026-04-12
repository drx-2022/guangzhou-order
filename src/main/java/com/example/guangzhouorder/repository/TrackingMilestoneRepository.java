package com.example.guangzhouorder.repository;

import com.example.guangzhouorder.entity.TrackingMilestone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrackingMilestoneRepository extends JpaRepository<TrackingMilestone, Long> {
    List<TrackingMilestone> findByTrackingTrackingIdOrderByStepOrderAsc(Long trackingId);
}
