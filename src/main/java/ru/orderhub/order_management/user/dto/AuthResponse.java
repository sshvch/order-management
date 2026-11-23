package ru.orderhub.order_management.user.dto;

/**
 * Ответ на регистрацию и логин: клиент сохраняет token и отправляет его в заголовке
 * Authorization: Bearer &lt;token&gt;
 */
public record AuthResponse(
        String token,
        String tokenType,
        long expiresIn,
        String email,
        String role
) {
}
