package ru.orderhub.order_management.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddCartItemRequest(
        @NotNull(message = "Id товара обязателен")
        Long productId,

        @NotNull(message = "Количество обязательно")
        @Min(value = 1, message = "Количество должно быть не меньше 1")
        @Max(value = 100, message = "За один раз можно добавить не больше 100 штук")
        Integer quantity
) {
}
