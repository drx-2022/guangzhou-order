package com.example.guangzhouorder.dto;

import com.example.guangzhouorder.entity.OrderTracking;
import com.example.guangzhouorder.entity.TrackingMilestone;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class OrderTrackingDto {

    private final Long trackingId;
    private final String trackingNumber;
    private final String carrier;
    private final String currentStatus;
    private final String currentStatusLabel;
    private final String currentLocation;
    private final String originLocation;
    private final String destinationLocation;
    private final LocalDateTime estimatedDelivery;
    private final LocalDateTime actualDelivery;
    private final boolean isDelivered;
    private final int progressPercent;
    private final List<MilestoneDto> milestones;

    public OrderTrackingDto(OrderTracking tracking, List<TrackingMilestone> milestones) {
        this.trackingId = tracking.getTrackingId();
        this.trackingNumber = tracking.getTrackingNumber();
        this.carrier = tracking.getCarrier();
        this.currentStatus = tracking.getCurrentStatus();
        this.currentStatusLabel = toStatusLabel(tracking.getCurrentStatus());
        this.currentLocation = tracking.getCurrentLocation();
        this.originLocation = tracking.getOriginLocation();
        this.destinationLocation = tracking.getDestinationLocation();
        this.estimatedDelivery = tracking.getEstimatedDelivery();
        this.actualDelivery = tracking.getActualDelivery();
        this.isDelivered = tracking.isDelivered();
        this.progressPercent = calculateProgress(milestones);
        this.milestones = milestones.stream().map(MilestoneDto::new).toList();
    }

    private static String toStatusLabel(String status) {
        if (status == null) return "Unknown";
        return switch (status) {
            case "PICKED_UP" -> "Picked Up";
            case "AT_FACTORY" -> "At Factory";
            case "CUSTOMS_EXPORT" -> "Export Customs";
            case "IN_TRANSIT" -> "In Transit";
            case "CUSTOMS_IMPORT" -> "Import Customs";
            case "OUT_FOR_DELIVERY" -> "Out for Delivery";
            case "DELIVERED" -> "Delivered";
            default -> status;
        };
    }

    private static int calculateProgress(List<TrackingMilestone> milestones) {
        if (milestones.isEmpty()) return 0;
        long completed = milestones.stream().filter(TrackingMilestone::isCompleted).count();
        return (int) ((completed * 100) / milestones.size());
    }

    @Getter
    public static class MilestoneDto {
        private final int stepOrder;
        private final String status;
        private final String statusLabel;
        private final String location;
        private final String description;
        private final LocalDateTime timestamp;
        private final boolean isCompleted;
        private final boolean isCurrent;

        public MilestoneDto(TrackingMilestone milestone) {
            this.stepOrder = milestone.getStepOrder();
            this.status = milestone.getStatus();
            this.statusLabel = milestone.getStatusLabel();
            this.location = milestone.getLocation();
            this.description = milestone.getDescription();
            this.timestamp = milestone.getTimestamp();
            this.isCompleted = milestone.isCompleted();
            this.isCurrent = milestone.isCurrent();
        }
    }
}
