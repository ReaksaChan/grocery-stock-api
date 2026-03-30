package com.reaksa.e_wingshop_api.dto.response;

import com.reaksa.e_wingshop_api.entity.ExpiryDiscount;
import com.reaksa.e_wingshop_api.enums.DiscountStatus;
import com.reaksa.e_wingshop_api.enums.DiscountTier;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpiryDiscountResponse {

    private Long          id;
    private Long          inventoryId;
    private Long          branchId;
    private String        branchName;
    private Long          productId;
    private String        productName;
    private String        productBarcode;
    private Integer       currentStock;
    private DiscountTier tier;
    private String        tierLabel;
    private BigDecimal    originalPrice;
    private BigDecimal    discountPct;
    private BigDecimal    discountedPrice;
    private BigDecimal    savingsAmount;
    private LocalDate     expiryDate;
    private LocalDate     validUntil;
    private Long          daysUntilExpiry;
    private DiscountStatus status;
    private String        note;
    private String        createdByEmail;
    private LocalDateTime createdAt;
    private LocalDateTime revokedAt;

    public static ExpiryDiscountResponse from(ExpiryDiscount d) {
        if (d == null) return null;

        BigDecimal original = d.getInventory().getProduct().getSellingPrice();
        BigDecimal savings  = original.subtract(d.getDiscountedPrice());

        Long daysUntilExpiry = null;
        if (d.getInventory().getExpiryDate() != null) {
            daysUntilExpiry = ChronoUnit.DAYS.between(
                    LocalDate.now(), d.getInventory().getExpiryDate());
        }

        return ExpiryDiscountResponse.builder()
                .id(d.getId())
                .inventoryId(d.getInventory().getId())
                .branchId(d.getInventory().getBranch().getId())
                .branchName(d.getInventory().getBranch().getName())
                .productId(d.getInventory().getProduct().getId())
                .productName(d.getInventory().getProduct().getName())
                .productBarcode(d.getInventory().getProduct().getBarcode())
                .currentStock(d.getInventory().getQuantity())
                .tier(d.getTier())
                .tierLabel(d.getTier().getLabel())
                .originalPrice(original)
                .discountPct(d.getDiscountPct())
                .discountedPrice(d.getDiscountedPrice())
                .savingsAmount(savings)
                .expiryDate(d.getInventory().getExpiryDate())
                .validUntil(d.getValidUntil())
                .daysUntilExpiry(daysUntilExpiry)
                .status(d.getStatus())
                .note(d.getNote())
                .createdByEmail(d.getCreatedByEmail())
                .createdAt(d.getCreatedAt())
                .revokedAt(d.getRevokedAt())
                .build();
    }
}
