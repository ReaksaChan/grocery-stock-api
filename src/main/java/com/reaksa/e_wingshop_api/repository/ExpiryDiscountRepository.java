package com.reaksa.e_wingshop_api.repository;

import com.reaksa.e_wingshop_api.entity.ExpiryDiscount;
import com.reaksa.e_wingshop_api.enums.DiscountStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExpiryDiscountRepository extends JpaRepository<ExpiryDiscount, Long> {

    /** Find the active discount on a specific inventory record. */
    Optional<ExpiryDiscount> findByInventoryIdAndStatus(Long inventoryId, DiscountStatus status);

    /** All active discounts, optionally filtered by branch. */
    @Query("""
        SELECT d FROM ExpiryDiscount d
        JOIN FETCH d.inventory i
        JOIN FETCH i.product p
        JOIN FETCH i.branch b
        WHERE d.status = 'ACTIVE'
        AND d.validUntil >= :today
        AND (:branchId IS NULL OR i.branch.id = :branchId)
        ORDER BY d.discountPct DESC
        """)
    List<ExpiryDiscount> findActiveDiscounts(@Param("today") LocalDate today,
                                              @Param("branchId") Long branchId);

    /** Paged active discounts for admin dashboard. */
    @Query("""
        SELECT d FROM ExpiryDiscount d
        JOIN FETCH d.inventory i
        JOIN FETCH i.product p
        JOIN FETCH i.branch b
        WHERE d.status = 'ACTIVE'
        AND d.validUntil >= :today
        AND (:branchId IS NULL OR i.branch.id = :branchId)
        """)
    Page<ExpiryDiscount> findActiveDiscountsPaged(@Param("today") LocalDate today,
                                                   @Param("branchId") Long branchId,
                                                   Pageable pageable);

    /** All discount history for one inventory record. */
    List<ExpiryDiscount> findByInventoryIdOrderByCreatedAtDesc(Long inventoryId);

    /** Discounts whose validUntil has passed — used by scheduler to auto-expire. */
    @Query("""
        SELECT d FROM ExpiryDiscount d
        WHERE d.status = 'ACTIVE'
        AND d.validUntil < :today
        """)
    List<ExpiryDiscount> findExpiredActiveDiscounts(@Param("today") LocalDate today);

    /** Discounts on inventories expiring within N days — for auto-suggest. */
    @Query("""
        SELECT d FROM ExpiryDiscount d
        JOIN d.inventory i
        WHERE i.expiryDate BETWEEN :today AND :cutoff
        AND d.status = 'ACTIVE'
        """)
    List<ExpiryDiscount> findDiscountsExpiringSoon(@Param("today") LocalDate today,
                                                    @Param("cutoff") LocalDate cutoff);

    /** Check if inventory already has an active discount. */
    boolean existsByInventoryIdAndStatus(Long inventoryId, DiscountStatus status);

    /** Auto-expire discounts in bulk. */
    @Modifying
    @Query("""
        UPDATE ExpiryDiscount d SET d.status = 'EXPIRED'
        WHERE d.status = 'ACTIVE' AND d.validUntil < :today
        """)
    int bulkExpireDiscounts(@Param("today") LocalDate today);
}
