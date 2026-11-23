package ru.orderhub.order_management.cart.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.orderhub.order_management.cart.dto.AddCartItemRequest;
import ru.orderhub.order_management.cart.dto.CartResponse;
import ru.orderhub.order_management.cart.dto.UpdateCartItemRequest;
import ru.orderhub.order_management.cart.service.CartService;

@RestController
@RequestMapping("/api/cart")
@Tag(name = "Cart", description = "Корзина текущего пользователя")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    @Operation(summary = "Моя корзина")
    public CartResponse getCart(Authentication authentication) {
        return cartService.getCart(authentication.getName());
    }

    @PostMapping("/items")
    @Operation(summary = "Добавить товар в корзину")
    public CartResponse addItem(Authentication authentication,
                                @Valid @RequestBody AddCartItemRequest request) {
        return cartService.addItem(authentication.getName(), request);
    }

    @PutMapping("/items/{itemId}")
    @Operation(summary = "Изменить количество товара в корзине")
    public CartResponse updateItem(Authentication authentication,
                                   @PathVariable Long itemId,
                                   @Valid @RequestBody UpdateCartItemRequest request) {
        return cartService.updateItem(authentication.getName(), itemId, request);
    }

    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "Удалить товар из корзины")
    public CartResponse removeItem(Authentication authentication, @PathVariable Long itemId) {
        return cartService.removeItem(authentication.getName(), itemId);
    }

    @DeleteMapping
    @Operation(summary = "Очистить корзину")
    public CartResponse clear(Authentication authentication) {
        return cartService.clear(authentication.getName());
    }
}
