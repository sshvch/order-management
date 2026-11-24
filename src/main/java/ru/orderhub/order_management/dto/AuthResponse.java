package ru.orderhub.order_management.dto;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresIn,
        String email,
        String role
) {
}
