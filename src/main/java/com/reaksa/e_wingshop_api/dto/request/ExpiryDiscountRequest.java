package com.reaksa.e_wingshop_api.dto.request;

import com.reaksa.e_wingshop_api.enums.DiscountTier;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ExpiryDiscountRequest {

    /** The inventory record to discount. */
    @NotNull(message = "Inventory ID is required")
    @Positive
    private Long inventoryId;

    /** Predefined tier — determines daysWindow and default discount rate. */
    @NotNull(message = "Discount tier is required")
    private DiscountTier tier;

    /**
     * Override the tier's default rate (0.01 – 99.99).
     * If null, the tier's defaultRatePct is used.
     */
    @DecimalMin(value = "0.01", message = "Discount must be at least 0.01%")
    @DecimalMax(value = "99.99", message = "Discount cannot exceed 99.99%")
    private BigDecimal discountPct;

    /**
     * Override the expiry cutoff date.
     * If null, defaults to inventory.expiryDate.
     * Only used when tier = CUSTOM.
     */
    private LocalDate validUntil;

    /** Optional admin note. */
    private String note;
}
