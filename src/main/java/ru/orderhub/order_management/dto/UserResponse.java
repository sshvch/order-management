package ru.orderhub.order_management.dto;

import ru.orderhub.order_management.entity.User;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String email,
        String role,
        LocalDateTime createdAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                user.getCreatedAt()
        );
    }
}
