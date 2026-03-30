package com.reaksa.e_wingshop_api.dto.response;

import com.reaksa.e_wingshop_api.entity.Product;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ProductResponse {
    private Long             id;
    private String           name;
    private String           description;
    private String           barcode;
    private String           imageUrl;
    private BigDecimal       costPrice;
    private BigDecimal       sellingPrice;
    private Boolean          isActive;
    private LocalDateTime createdAt;
    private CategoryResponse category;   // flat summary — no products list inside

    public static ProductResponse from(Product product) {
        if (product == null) return null;
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .barcode(product.getBarcode())
                .imageUrl(product.getImageUrl())
                .costPrice(product.getCostPrice())
                .sellingPrice(product.getSellingPrice())
                .isActive(product.getIsActive())
                .createdAt(product.getCreatedAt())
                .category(CategoryResponse.from(product.getCategory()))
                .build();
    }
}
