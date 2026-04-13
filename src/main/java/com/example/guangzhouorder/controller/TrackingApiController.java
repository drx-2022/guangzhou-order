package com.example.guangzhouorder.controller;

import com.example.guangzhouorder.service.OrderTrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
public class TrackingApiController {

    private final OrderTrackingService orderTrackingService;

    @PatchMapping("/{trackingId}/milestone/{stepOrder}")
    public ResponseEntity<?> updateMilestone(
            @PathVariable Long trackingId,
            @PathVariable int stepOrder,
            @RequestParam boolean completed,
            @RequestParam(required = false) String timestamp) {
        try {
            orderTrackingService.updateMilestoneStatus(trackingId, stepOrder, completed, timestamp);
            return ResponseEntity.ok(Map.of("success", true, "message", "Milestone updated"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<?> getTrackingByOrder(@PathVariable Long orderId) {
        var tracking = orderTrackingService.getTrackingByOrderId(orderId);
        if (tracking == null) {
            return ResponseEntity.notFound().build();
        }
        var milestones = orderTrackingService.getMilestones(tracking.getTrackingId());
        return ResponseEntity.ok(Map.of(
            "tracking", tracking,
            "milestones", milestones
        ));
    }
}
