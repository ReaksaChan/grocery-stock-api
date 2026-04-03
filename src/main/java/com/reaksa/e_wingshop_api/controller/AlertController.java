package com.reaksa.e_wingshop_api.controller;

import com.reaksa.e_wingshop_api.service.AlertScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class AlertController {
    private final AlertScheduler alertScheduler;

    // 🔔 Trigger expiry check manually
    @PostMapping("/expiry")
    public ResponseEntity<String> triggerExpiryCheck() {
        alertScheduler.checkExpiryAlerts();
        return ResponseEntity.ok("Expiry alert check executed successfully.");
    }

    // ⚠️ Trigger low stock check manually
    @PostMapping("/low-stock")
    public ResponseEntity<String> triggerLowStockCheck() {
        alertScheduler.checkLowStockAlerts();
        return ResponseEntity.ok("Low stock alert check executed successfully.");
    }
}
