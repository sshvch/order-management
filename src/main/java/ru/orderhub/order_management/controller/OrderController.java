package ru.orderhub.order_management.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.orderhub.order_management.dto.PageResponse;
import ru.orderhub.order_management.dto.OrderResponse;
import ru.orderhub.order_management.dto.OrderSummaryResponse;
import ru.orderhub.order_management.dto.UpdateOrderStatusRequest;
import ru.orderhub.order_management.service.OrderService;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Заказы")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @Operation(summary = "Оформить заказ из корзины")
    public ResponseEntity<OrderResponse> create(Authentication authentication) {
        OrderResponse response = orderService.createOrder(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "История моих заказов")
    public PageResponse<OrderSummaryResponse> getMyOrders(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return orderService.getMyOrders(authentication.getName(), page, size);
    }

    @GetMapping("/all")
    @Operation(summary = "Все заказы (только ADMIN)")
    public PageResponse<OrderSummaryResponse> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return orderService.getAllOrders(page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Заказ по id (свой — пользователю, любой — админу)")
    public OrderResponse getById(Authentication authentication, @PathVariable Long id) {
        return orderService.getOrder(id, authentication.getName(), isAdmin(authentication));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Сменить статус заказа (только ADMIN)")
    public OrderResponse updateStatus(@PathVariable Long id,
                                      @Valid @RequestBody UpdateOrderStatusRequest request) {
        return orderService.updateStatus(id, request.status());
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }
}
