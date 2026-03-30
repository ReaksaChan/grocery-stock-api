package com.reaksa.e_wingshop_api.dto.response;

import com.reaksa.e_wingshop_api.entity.User;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserResponse {
    private Long          id;
    private String        fullName;
    private String        email;
    private String        phone;
    private String        role;
    private LocalDateTime createdAt;

    public static UserResponse from(User user) {
        if (user == null) return null;
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole().getName().name())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
