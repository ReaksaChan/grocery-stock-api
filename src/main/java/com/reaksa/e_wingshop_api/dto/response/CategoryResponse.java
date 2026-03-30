package com.reaksa.e_wingshop_api.dto.response;

import com.reaksa.e_wingshop_api.entity.Category;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CategoryResponse {
    private Long   id;
    private String name;
    private String description;

    public static CategoryResponse from(Category category) {
        if (category == null) return null;
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .build();
    }
}
