package com.reaksa.e_wingshop_api.entity;

import com.reaksa.e_wingshop_api.enums.DiscountStatus;
import com.reaksa.e_wingshop_api.enums.DiscountTier;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "expiry_discounts", indexes = {
        @Index(name = "idx_ed_inventory",  columnList = "inventory_id"),
        @Index(name = "idx_ed_status",     columnList = "status"),
        @Index(name = "idx_ed_valid_until", columnList = "valid_until")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExpiryDiscount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The inventory record this discount applies to. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id", nullable = false)
    private Inventory inventory;

    /** Which predefined tier was used (or CUSTOM). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DiscountTier tier;

    /** The actual discount percentage (0–100). Can differ from tier default. */
    @Column(name = "discount_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPct;

    /** Selling price after discount = sellingPrice × (1 − discountPct/100). */
    @Column(name = "discounted_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountedPrice;

    /** Discount active until this date (usually = inventory.expiryDate). */
    @Column(name = "valid_until", nullable = false)
    private LocalDate validUntil;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private DiscountStatus status = DiscountStatus.ACTIVE;

    /** Admin note (optional). */
    @Column(columnDefinition = "TEXT")
    private String note;

    /** Who created this discount. */
    @Column(name = "created_by_id")
    private Long createdById;

    @Column(name = "created_by_email", length = 150)
    private String createdByEmail;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    // ── Convenience ───────────────────────────────────────────────────

    public boolean isCurrentlyActive() {
        return status == DiscountStatus.ACTIVE
                && !LocalDate.now().isAfter(validUntil);
    }

    /**
     * Compute discounted price from original selling price.
     * discountedPrice = sellingPrice × (1 − discountPct / 100), rounded to 2dp.
     */
    public static BigDecimal computeDiscountedPrice(BigDecimal sellingPrice, BigDecimal discountPct) {
        BigDecimal multiplier = BigDecimal.ONE
                .subtract(discountPct.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        return sellingPrice.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }
}
