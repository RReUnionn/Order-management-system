package com.vupl.userservice.dto.response;

import com.vupl.userservice.entity.User;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder
public class UserResponse {
    private String id;
    private String email;
    private String phone;
    private String fullName;
    private String role;
    private String status;
    private LocalDateTime createdAt;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId()).email(user.getEmail()).phone(user.getPhone())
                .fullName(user.getFullName()).role(user.getRole().getName())
                .status(user.getStatus().name()).createdAt(user.getCreatedAt()).build();
    }
}