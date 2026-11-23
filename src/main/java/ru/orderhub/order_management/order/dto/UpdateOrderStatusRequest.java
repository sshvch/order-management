package ru.orderhub.order_management.order.dto;

import jakarta.validation.constraints.NotNull;
import ru.orderhub.order_management.order.entity.OrderStatus;

public record UpdateOrderStatusRequest(
        @NotNull(message = "Статус обязателен")
        OrderStatus status
) {
}
