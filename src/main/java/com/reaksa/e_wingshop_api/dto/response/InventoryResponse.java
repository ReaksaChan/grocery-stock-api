package com.reaksa.e_wingshop_api.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class InventoryResponse {
    private Long      id;
    private Long      branchId;
    private String    branchName;
    private Long      productId;
    private String    productName;
    private String    productBarcode;
    private Integer   quantity;
    private Integer   lowStockThreshold;
    private LocalDate expiryDate;
    private Long      daysUntilExpiry;   // negative = already expired
    private boolean   lowStock;
    private boolean   expired;
    private LocalDateTime updatedAt;

//    public static InventoryResponse from(Inventory inv) {
//        if (inv == null) return null;
//
//        Long daysUntil = null;
//        if (inv.getExpiryDate() != null) {
//            daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), inv.getExpiryDate());
//        }
//
//        return InventoryResponse.builder()
//                .id(inv.getId())
//                .branchId(inv.getBranch().getId())
//                .branchName(inv.getBranch().getName())
//                .productId(inv.getProduct().getId())
//                .productName(inv.getProduct().getName())
//                .productBarcode(inv.getProduct().getBarcode())
//                .quantity(inv.getQuantity())
//                .lowStockThreshold(inv.getLowStockThreshold())
//                .expiryDate(inv.getExpiryDate())
//                .daysUntilExpiry(daysUntil)
//                .lowStock(inv.isLowStock())
//                .expired(inv.isExpired())
//                .updatedAt(inv.getUpdatedAt())
//                .build();
//    }
}
