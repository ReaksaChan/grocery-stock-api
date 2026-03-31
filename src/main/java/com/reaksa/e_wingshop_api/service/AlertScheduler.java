package com.reaksa.e_wingshop_api.service;

import com.reaksa.e_wingshop_api.entity.Inventory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertScheduler {

    private final InventoryService inventoryService;

    /**
     * Daily at 07:00 — log all items expiring within 7 days.
     * Replace log calls with email/push/webhook notifications in production.
     */
    @Scheduled(cron = "${app.inventory.expiry-check-cron}")
    public void checkExpiryAlerts() {
        List<Inventory> expiringSoon = inventoryService.getExpiringSoon(null, 7);
        List<Inventory> expired      = inventoryService.getExpired();

        if (!expiringSoon.isEmpty()) {
            log.warn("EXPIRY ALERT — {} item(s) expire within 7 days:", expiringSoon.size());
            expiringSoon.forEach(inv -> log.warn("  Branch={} Product={} Qty={} ExpiryDate={}",
                    inv.getBranch().getName(),
                    inv.getProduct().getName(),
                    inv.getQuantity(),
                    inv.getExpiryDate()));
        }

        if (!expired.isEmpty()) {
            log.error("EXPIRED STOCK — {} item(s) past expiry date:", expired.size());
            expired.forEach(inv -> log.error("  Branch={} Product={} Qty={} ExpiryDate={}",
                    inv.getBranch().getName(),
                    inv.getProduct().getName(),
                    inv.getQuantity(),
                    inv.getExpiryDate()));
        }
    }

    /**
     * Every 30 minutes — log items at or below low-stock threshold.
     */
    @Scheduled(cron = "${app.inventory.low-stock-check-cron}")
    public void checkLowStockAlerts() {
        List<Inventory> lowStock = inventoryService.getLowStock(null);

        if (!lowStock.isEmpty()) {
            log.warn("LOW STOCK ALERT — {} item(s) below threshold:", lowStock.size());
            lowStock.forEach(inv -> log.warn("  Branch={} Product={} Qty={} Threshold={}",
                    inv.getBranch().getName(),
                    inv.getProduct().getName(),
                    inv.getQuantity(),
                    inv.getLowStockThreshold()));
        }
    }
}
