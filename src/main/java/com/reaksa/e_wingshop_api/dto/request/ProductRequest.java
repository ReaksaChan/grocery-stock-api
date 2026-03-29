package com.reaksa.e_wingshop_api.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(max = 150)
    private String name;

    private String description;

    @Size(max = 50)
    private String barcode;

    private String imageUrl;

    @NotNull(message = "Category is required")
    @Positive
    private Long categoryId;

    @NotNull(message = "Cost price is required")
    @DecimalMin(value = "0.0", inclusive = true)
    private java.math.BigDecimal costPrice;

    @NotNull(message = "Selling price is required")
    @DecimalMin(value = "0.0", inclusive = true)
    private java.math.BigDecimal sellingPrice;

    private Boolean isActive = true;
}
