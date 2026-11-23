package ru.orderhub.order_management.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank(message = "Название обязательно")
        @Size(max = 100, message = "Название не длиннее 100 символов")
        String name,

        @Size(max = 500, message = "Описание не длиннее 500 символов")
        String description
) {
}
