package ru.orderhub.order_management.product.dto;

import ru.orderhub.order_management.product.entity.Category;

public record CategoryResponse(
        Long id,
        String name,
        String description
) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription()
        );
    }
}
