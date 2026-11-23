package ru.orderhub.order_management.dto;

import jakarta.validation.constraints.NotNull;
import ru.orderhub.order_management.entity.OrderStatus;

public record UpdateOrderStatusRequest(
        @NotNull(message = "Статус обязателен")
        OrderStatus status
) {
}
