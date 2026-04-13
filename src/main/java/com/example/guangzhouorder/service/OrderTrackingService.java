package com.example.guangzhouorder.service;

import com.example.guangzhouorder.entity.Order;
import com.example.guangzhouorder.entity.OrderTracking;
import com.example.guangzhouorder.entity.TrackingMilestone;
import com.example.guangzhouorder.repository.OrderRepository;
import com.example.guangzhouorder.repository.OrderTrackingRepository;
import com.example.guangzhouorder.repository.TrackingMilestoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class OrderTrackingService {

    private final OrderTrackingRepository trackingRepository;
    private final TrackingMilestoneRepository milestoneRepository;
    private final OrderRepository orderRepository;
    private final Random random = new Random();

    public OrderTracking getTrackingByOrderId(Long orderId) {
        return trackingRepository.findByOrderOrderId(orderId)
                .orElse(null);
    }

    @Transactional
    public OrderTracking getOrCreateTracking(Long orderId) {
        return trackingRepository.findByOrderOrderId(orderId)
                .orElseGet(() -> createFakeTracking(orderId));
    }

    @Transactional
    public OrderTracking createFakeTracking(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (trackingRepository.existsByOrderOrderId(orderId)) {
            return trackingRepository.findByOrderOrderId(orderId).get();
        }

        String trackingNumber = "GZD" + System.currentTimeMillis() % 1000000;

        OrderTracking tracking = OrderTracking.builder()
                .order(order)
                .trackingNumber(trackingNumber)
                .carrier("Guangzhou Direct Logistics")
                .currentStatus("PICKED_UP")
                .originLocation("Guangzhou, China")
                .destinationLocation("Ho Chi Minh City, Vietnam")
                .currentLocation("Guangzhou Warehouse")
                .estimatedDelivery(LocalDateTime.now().plusDays(5 + random.nextInt(10)))
                .isDelivered(false)
                .build();

        tracking = trackingRepository.save(tracking);

        List<TrackingMilestone> milestones = List.of(
            createMilestone(tracking, 1, "PICKED_UP", "Picked Up", "Guangzhou Warehouse", "Package picked up from factory", true, false),
            createMilestone(tracking, 2, "AT_FACTORY", "At Factory", "Guangzhou Sorting Center", "Package processed at sorting facility", false, false),
            createMilestone(tracking, 3, "CUSTOMS_EXPORT", "Export Customs", "Shenzhen Customs", "Cleared Chinese export customs", false, false),
            createMilestone(tracking, 4, "IN_TRANSIT", "In Transit", "In Transit", "Package in transit to destination", false, false),
            createMilestone(tracking, 5, "CUSTOMS_IMPORT", "Import Customs", "Ho Chi Minh City", "Pending Vietnam import customs", false, false),
            createMilestone(tracking, 6, "OUT_FOR_DELIVERY", "Out for Delivery", "HCMC Distribution Center", "Out for delivery to customer", false, false),
            createMilestone(tracking, 7, "DELIVERED", "Delivered", "Customer Address", "Package delivered successfully", false, false)
        );

        milestoneRepository.saveAll(milestones);
        return tracking;
    }

    private TrackingMilestone createMilestone(OrderTracking tracking, int step, String status, String label, String location, String desc, boolean completed, boolean current) {
        return TrackingMilestone.builder()
                .tracking(tracking)
                .stepOrder(step)
                .status(status)
                .statusLabel(label)
                .location(location)
                .description(desc)
                .timestamp(completed ? LocalDateTime.now().minusHours(random.nextInt(48)) : null)
                .isCompleted(completed)
                .isCurrent(current)
                .build();
    }

    public List<TrackingMilestone> getMilestones(Long trackingId) {
        return milestoneRepository.findByTrackingTrackingIdOrderByStepOrderAsc(trackingId);
    }

    public int getProgressPercent(Long orderId) {
        OrderTracking tracking = trackingRepository.findByOrderOrderId(orderId).orElse(null);
        if (tracking == null) return 0;
        
        List<TrackingMilestone> milestones = milestoneRepository.findByTrackingTrackingIdOrderByStepOrderAsc(tracking.getTrackingId());
        long completed = milestones.stream().filter(TrackingMilestone::isCompleted).count();
        return (int) ((completed * 100) / milestones.size());
    }

    @Transactional
    public void updateMilestoneStatus(Long trackingId, int stepOrder, boolean completed, String timestamp) {
        TrackingMilestone milestone = milestoneRepository.findByTrackingTrackingIdOrderByStepOrderAsc(trackingId)
                .stream()
                .filter(m -> m.getStepOrder() == stepOrder)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Milestone not found"));

        milestone.setCompleted(completed);
        if (timestamp != null) {
            milestone.setTimestamp(LocalDateTime.parse(timestamp));
        }
        milestoneRepository.save(milestone);

        if (completed) {
            milestoneRepository.findByTrackingTrackingIdOrderByStepOrderAsc(trackingId)
                    .stream()
                    .filter(m -> m.getStepOrder() == stepOrder + 1)
                    .findFirst()
                    .ifPresent(next -> {
                        next.setCurrent(true);
                        milestoneRepository.save(next);
                    });
        }

        OrderTracking tracking = trackingRepository.findById(trackingId)
                .orElseThrow(() -> new RuntimeException("Tracking not found"));
        tracking.setCurrentLocation(milestone.getLocation());
        tracking.setCurrentStatus(milestone.getStatus());
        
        if (stepOrder == 7 && completed) {
            tracking.setDelivered(true);
            tracking.setActualDelivery(LocalDateTime.now());
        }
        
        trackingRepository.save(tracking);
    }
}
