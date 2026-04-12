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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderTrackingService {

    private final OrderTrackingRepository trackingRepository;
    private final TrackingMilestoneRepository milestoneRepository;
    private final OrderRepository orderRepository;
    private final Random random = new Random();

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
                .currentStatus("IN_TRANSIT")
                .originLocation("Guangzhou, China")
                .destinationLocation("Ho Chi Minh City, Vietnam")
                .currentLocation(getRandomLocation())
                .estimatedDelivery(LocalDateTime.now().plusDays(5 + random.nextInt(10)))
                .isDelivered(false)
                .build();

        tracking = trackingRepository.save(tracking);

        List<TrackingMilestone> milestones = generateFakeMilestones(tracking);
        milestoneRepository.saveAll(milestones);

        return tracking;
    }

    private List<TrackingMilestone> generateFakeMilestones(OrderTracking tracking) {
        LocalDateTime now = LocalDateTime.now();
        Order order = tracking.getOrder();
        boolean isDelivered = "DONE".equals(order.getStatus());

        return List.of(
            TrackingMilestone.builder()
                .tracking(tracking)
                .stepOrder(1)
                .status("PICKED_UP")
                .statusLabel("Picked Up")
                .location("Guangzhou Warehouse")
                .description("Package picked up from factory in Baiyun District")
                .timestamp(now.minusDays(3 + random.nextInt(3)))
                .isCompleted(true)
                .isCurrent(false)
                .build(),
            TrackingMilestone.builder()
                .tracking(tracking)
                .stepOrder(2)
                .status("AT_FACTORY")
                .statusLabel("At Factory")
                .location("Guangzhou Sorting Center")
                .description("Package processed at Guangzhou sorting facility")
                .timestamp(now.minusDays(2 + random.nextInt(2)))
                .isCompleted(true)
                .isCurrent(false)
                .build(),
            TrackingMilestone.builder()
                .tracking(tracking)
                .stepOrder(3)
                .status("CUSTOMS_EXPORT")
                .statusLabel("Export Customs")
                .location("Shenzhen Customs")
                .description("Cleared Chinese export customs - awaiting transport")
                .timestamp(now.minusDays(1 + random.nextInt(2)))
                .isCompleted(true)
                .isCurrent(false)
                .build(),
            TrackingMilestone.builder()
                .tracking(tracking)
                .stepOrder(4)
                .status("IN_TRANSIT")
                .statusLabel(isDelivered ? "In Transit" : "In Transit")
                .location(tracking.getCurrentLocation())
                .description(isDelivered ? "Package in transit to destination" : "Package currently in transit via land route")
                .timestamp(now.minusHours(random.nextInt(24)))
                .isCompleted(isDelivered)
                .isCurrent(!isDelivered)
                .build(),
            TrackingMilestone.builder()
                .tracking(tracking)
                .stepOrder(5)
                .status("CUSTOMS_IMPORT")
                .statusLabel("Import Customs")
                .location("Ho Chi Minh City")
                .description(isDelivered ? "Cleared Vietnam import customs" : "Pending Vietnam import customs clearance")
                .timestamp(isDelivered ? now.minusHours(12) : null)
                .isCompleted(isDelivered)
                .isCurrent(!isDelivered)
                .build(),
            TrackingMilestone.builder()
                .tracking(tracking)
                .stepOrder(6)
                .status("OUT_FOR_DELIVERY")
                .statusLabel("Out for Delivery")
                .location("Ho Chi Minh City Distribution Center")
                .description(isDelivered ? "Out for delivery to customer" : "Estimated delivery soon")
                .timestamp(isDelivered ? now.minusHours(6) : null)
                .isCompleted(isDelivered)
                .isCurrent(!isDelivered)
                .build(),
            TrackingMilestone.builder()
                .tracking(tracking)
                .stepOrder(7)
                .status("DELIVERED")
                .statusLabel("Delivered")
                .location("Customer Address")
                .description(isDelivered ? "Package delivered successfully" : "Pending delivery")
                .timestamp(isDelivered ? now : null)
                .isCompleted(isDelivered)
                .isCurrent(false)
                .build()
        );
    }

    private String getRandomLocation() {
        List<String> locations = List.of(
            "Shenzhen - Dongguan Highway",
            "Guangzhou - Foshan Route",
            "Hanoi - Ha Long Expressway",
            "Nanning Border Crossing",
            "Lang Son Checkpoint",
            "Bac Ninh Distribution Hub",
            "Hai Phong Port",
            "Hanoi Logistics Center"
        );
        return locations.get(random.nextInt(locations.size()));
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
}
