package ru.orderhub.order_management.dto;

import ru.orderhub.order_management.entity.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Короткая карточка заказа для списка — без позиций,
 * чтобы не тянуть из базы лишние данные.
 */
public record OrderSummaryResponse(
        Long id,
        String status,
        BigDecimal totalAmount,
        LocalDateTime createdAt
) {

    public static OrderSummaryResponse from(Order order) {
        return new OrderSummaryResponse(
                order.getId(),
                order.getStatus().name(),
                order.getTotalAmount(),
                order.getCreatedAt()
        );
    }
}
