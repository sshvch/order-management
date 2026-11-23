package ru.orderhub.order_management.common.exception;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Единый формат ошибки для всех эндпоинтов.
 */
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        List<String> details
) {
}
