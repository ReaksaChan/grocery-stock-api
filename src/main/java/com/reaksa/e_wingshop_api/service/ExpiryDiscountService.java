package com.reaksa.e_wingshop_api.service;

import com.reaksa.e_wingshop_api.dto.request.ExpiryDiscountRequest;
import com.reaksa.e_wingshop_api.entity.ExpiryDiscount;
import com.reaksa.e_wingshop_api.entity.Inventory;
import com.reaksa.e_wingshop_api.enums.DiscountStatus;
import com.reaksa.e_wingshop_api.enums.DiscountTier;
import com.reaksa.e_wingshop_api.exception.DuplicateResourceException;
import com.reaksa.e_wingshop_api.exception.ResourceNotFoundException;
import com.reaksa.e_wingshop_api.repository.ExpiryDiscountRepository;
import com.reaksa.e_wingshop_api.repository.InventoryRepository;
import com.reaksa.e_wingshop_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpiryDiscountService {

    private final ExpiryDiscountRepository discountRepository;
    private final InventoryRepository inventoryRepository;
    private final UserRepository userRepository;

    // ── Create discount ───────────────────────────────────────────────

    /**
     * Apply an expiry discount to an inventory record.
     *
     * Rules:
     * 1. Inventory must exist and have an expiry date.
     * 2. Only one ACTIVE discount per inventory record at a time.
     * 3. For non-CUSTOM tiers, validUntil = inventory.expiryDate and
     *    discountPct defaults to tier.defaultRatePct unless overridden.
     * 4. For CUSTOM tier, both validUntil and discountPct must be supplied.
     */
    @Transactional
    public ExpiryDiscount createDiscount(ExpiryDiscountRequest request,
                                         Long actorId, String actorEmail) {
        Inventory inv = inventoryRepository.findById(request.getInventoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventory", request.getInventoryId()));

        if (inv.getExpiryDate() == null) {
            throw new IllegalArgumentException(
                    "Cannot create expiry discount — this inventory record has no expiry date.");
        }

        if (inv.isExpired()) {
            throw new IllegalArgumentException(
                    "Product '%s' is already expired (expiry: %s). Cannot discount an expired item."
                            .formatted(inv.getProduct().getName(), inv.getExpiryDate()));
        }

        if (discountRepository.existsByInventoryIdAndStatus(
                request.getInventoryId(), DiscountStatus.ACTIVE)) {
            throw new DuplicateResourceException(
                    "An active discount already exists for this inventory record. " +
                    "Revoke it first before creating a new one.");
        }

        // Resolve discount % — tier default or admin override
        BigDecimal discountPct = resolveDiscountPct(request);

        // Resolve validUntil — CUSTOM requires explicit date, others default to expiryDate
        LocalDate validUntil = resolveValidUntil(request, inv);

        BigDecimal discountedPrice = ExpiryDiscount.computeDiscountedPrice(
                inv.getProduct().getSellingPrice(), discountPct);

        ExpiryDiscount discount = ExpiryDiscount.builder()
                .inventory(inv)
                .tier(request.getTier())
                .discountPct(discountPct)
                .discountedPrice(discountedPrice)
                .validUntil(validUntil)
                .status(DiscountStatus.ACTIVE)
                .note(request.getNote())
                .createdById(actorId)
                .createdByEmail(actorEmail)
                .build();

        ExpiryDiscount saved = discountRepository.save(discount);

        log.info("Expiry discount created — inventory={} product='{}' branch='{}' pct={}% " +
                 "discountedPrice={} validUntil={} tier={} by={}",
                inv.getId(), inv.getProduct().getName(), inv.getBranch().getName(),
                discountPct, discountedPrice, validUntil, request.getTier(), actorEmail);

        return saved;
    }

    // ── Bulk auto-suggest ─────────────────────────────────────────────

    /**
     * Auto-create discounts for all inventory records expiring within a
     * given tier's window that don't already have an active discount.
     * Returns the number of new discounts created.
     */
    @Transactional
    public int autoApplyByTier(DiscountTier tier, Long branchId,
                               Long actorId, String actorEmail) {
        if (tier == DiscountTier.CUSTOM) {
            throw new IllegalArgumentException(
                    "CUSTOM tier cannot be used for auto-apply. Use createDiscount() instead.");
        }

        LocalDate today  = LocalDate.now();
        LocalDate cutoff = today.plusDays(tier.getDaysWindow());

        List<Inventory> candidates = inventoryRepository.findExpiringSoon(today, cutoff, branchId)
                .stream()
                .filter(inv -> !discountRepository.existsByInventoryIdAndStatus(
                        inv.getId(), DiscountStatus.ACTIVE))
                .filter(inv -> inv.getExpiryDate() != null && !inv.isExpired())
                .toList();

        int count = 0;
        for (Inventory inv : candidates) {
            BigDecimal discountPct = BigDecimal.valueOf(tier.getDefaultRatePct());
            BigDecimal discountedPrice = ExpiryDiscount.computeDiscountedPrice(
                    inv.getProduct().getSellingPrice(), discountPct);

            ExpiryDiscount discount = ExpiryDiscount.builder()
                    .inventory(inv)
                    .tier(tier)
                    .discountPct(discountPct)
                    .discountedPrice(discountedPrice)
                    .validUntil(inv.getExpiryDate())
                    .status(DiscountStatus.ACTIVE)
                    .note("Auto-applied for tier: " + tier.getLabel())
                    .createdById(actorId)
                    .createdByEmail(actorEmail)
                    .build();

            discountRepository.save(discount);
            count++;
        }

        log.info("Auto-apply discounts — tier={} branch={} created={}", tier, branchId, count);
        return count;
    }

    // ── Read ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ExpiryDiscount findById(Long id) {
        return discountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ExpiryDiscount", id));
    }

    @Transactional(readOnly = true)
    public List<ExpiryDiscount> getActiveDiscounts(Long branchId) {
        return discountRepository.findActiveDiscounts(LocalDate.now(), branchId);
    }

    @Transactional(readOnly = true)
    public Page<ExpiryDiscount> getActiveDiscountsPaged(Long branchId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("discountPct").descending());
        return discountRepository.findActiveDiscountsPaged(LocalDate.now(), branchId, pageable);
    }

    @Transactional(readOnly = true)
    public List<ExpiryDiscount> getHistoryForInventory(Long inventoryId) {
        return discountRepository.findByInventoryIdOrderByCreatedAtDesc(inventoryId);
    }

    /** Get the active discount for a specific inventory, if any. */
    @Transactional(readOnly = true)
    public Optional<ExpiryDiscount> getActiveForInventory(Long inventoryId) {
        return discountRepository.findByInventoryIdAndStatus(
                inventoryId, DiscountStatus.ACTIVE);
    }

    /**
     * Resolve the effective selling price for an inventory record at order time.
     * Returns discountedPrice if an active discount exists, otherwise sellingPrice.
     */
    @Transactional(readOnly = true)
    public BigDecimal resolveEffectivePrice(Inventory inv) {
        return discountRepository
                .findByInventoryIdAndStatus(inv.getId(), DiscountStatus.ACTIVE)
                .filter(ExpiryDiscount::isCurrentlyActive)
                .map(ExpiryDiscount::getDiscountedPrice)
                .orElse(inv.getProduct().getSellingPrice());
    }

    // ── Revoke ────────────────────────────────────────────────────────

    @Transactional
    public ExpiryDiscount revoke(Long discountId, String reason, Long actorId, String actorEmail) {
        ExpiryDiscount discount = findById(discountId);

        if (discount.getStatus() != DiscountStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Cannot revoke a discount that is not ACTIVE (current: " + discount.getStatus() + ")");
        }

        discount.setStatus(DiscountStatus.REVOKED);
        discount.setRevokedAt(LocalDateTime.now());
        discount.setNote((discount.getNote() != null ? discount.getNote() + " | " : "")
                + "Revoked by " + actorEmail + ": " + reason);

        log.info("Discount revoked — id={} by={} reason={}", discountId, actorEmail, reason);
        return discountRepository.save(discount);
    }

    // ── Scheduled auto-expiry ─────────────────────────────────────────

    /**
     * Runs daily at 07:30 — bulk-expires any ACTIVE discounts whose validUntil
     * has passed. This means a discount doesn't need to be manually cleaned up
     * when the product's expiry date passes.
     */
    @Scheduled(cron = "0 30 7 * * *")
    @Transactional
    public void autoExpireDiscounts() {
        int expired = discountRepository.bulkExpireDiscounts(LocalDate.now());
        if (expired > 0) {
            log.info("Auto-expired {} discount(s) past validUntil.", expired);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private BigDecimal resolveDiscountPct(ExpiryDiscountRequest request) {
        if (request.getTier() == DiscountTier.CUSTOM) {
            if (request.getDiscountPct() == null) {
                throw new IllegalArgumentException(
                        "discountPct is required when tier is CUSTOM.");
            }
            return request.getDiscountPct();
        }
        return request.getDiscountPct() != null
                ? request.getDiscountPct()
                : BigDecimal.valueOf(request.getTier().getDefaultRatePct());
    }

    private LocalDate resolveValidUntil(ExpiryDiscountRequest request, Inventory inv) {
        if (request.getTier() == DiscountTier.CUSTOM) {
            if (request.getValidUntil() == null) {
                throw new IllegalArgumentException(
                        "validUntil is required when tier is CUSTOM.");
            }
            return request.getValidUntil();
        }
        return inv.getExpiryDate();
    }
}
