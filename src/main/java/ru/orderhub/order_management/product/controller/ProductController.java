package ru.orderhub.order_management.product.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.orderhub.order_management.common.dto.PageResponse;
import ru.orderhub.order_management.product.dto.ProductRequest;
import ru.orderhub.order_management.product.dto.ProductResponse;
import ru.orderhub.order_management.product.service.ProductService;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Products", description = "Каталог товаров")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @Operation(summary = "Список товаров с поиском и пагинацией (доступно без входа)")
    public PageResponse<ProductResponse> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return productService.getAll(search, page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Товар по id (доступно без входа)")
    public ProductResponse getById(@PathVariable Long id) {
        return productService.getById(id);
    }

    @PostMapping
    @Operation(summary = "Создать товар (только ADMIN)")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить товар (только ADMIN)")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return productService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Скрыть товар из каталога (только ADMIN)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
