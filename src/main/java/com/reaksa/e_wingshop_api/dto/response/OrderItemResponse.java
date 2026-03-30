package com.reaksa.e_wingshop_api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class OrderItemResponse {
    private Long       id;
    private Long       productId;
    private String     productName;
    private String     productBarcode;
    private Integer    quantity;
    private BigDecimal price;
    private BigDecimal subtotal;

//    public static OrderItemResponse from(OrderItem item) {
//        if (item == null) return null;
//        return OrderItemResponse.builder()
//                .id(item.getId())
//                .productId(item.getProduct().getId())
//                .productName(item.getProduct().getName())
//                .productBarcode(item.getProduct().getBarcode())
//                .quantity(item.getQuantity())
//                .price(item.getPrice())
//                .subtotal(item.getSubtotal())
//                .build();
//    }
}
