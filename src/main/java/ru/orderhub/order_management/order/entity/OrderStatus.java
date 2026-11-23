package ru.orderhub.order_management.order.entity;

/**
 * Статусы заказа. Заказ проходит их по порядку, отменить можно не всё.
 */
public enum OrderStatus {
    CREATED,
    PAID,
    SHIPPED,
    DELIVERED,
    CANCELLED
}
