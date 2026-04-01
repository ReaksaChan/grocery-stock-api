package com.reaksa.e_wingshop_api.repository;

import com.reaksa.e_wingshop_api.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByBarcode(String barcode);

    Page<Product> findByCategoryIdAndIsActiveOrderByIdDesc(Long categoryId, Boolean isActive, Pageable pageable);

    Page<Product> findByIsActiveOrderByIdDesc(Boolean isActive, Pageable pageable);

    @Query("""
        SELECT p FROM Product p
        WHERE p.isActive = true
        AND (:categoryId IS NULL OR p.category.id = :categoryId)
        AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
             OR LOWER(COALESCE(p.barcode, '')) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY p.id DESC
        """)
    Page<Product> searchByKeyword(@Param("categoryId") Long categoryId,
                                  @Param("search") String search,
                                  Pageable pageable);
}
