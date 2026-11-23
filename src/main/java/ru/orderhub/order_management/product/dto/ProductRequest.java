package ru.orderhub.order_management.product.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank(message = "Название обязательно")
        @Size(max = 150, message = "Название не длиннее 150 символов")
        String name,

        @Size(max = 1000, message = "Описание не длиннее 1000 символов")
        String description,

        @NotNull(message = "Цена обязательна")
        @DecimalMin(value = "0.01", message = "Цена должна быть больше нуля")
        @Digits(integer = 8, fraction = 2, message = "Цена: до 8 цифр до точки и 2 после")
        BigDecimal price,

        @NotNull(message = "Количество обязательно")
        @Min(value = 0, message = "Количество не может быть отрицательным")
        Integer stockQuantity,

        Long categoryId
) {
}
