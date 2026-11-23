package ru.orderhub.order_management.product.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.orderhub.order_management.common.dto.PageResponse;
import ru.orderhub.order_management.common.exception.BadRequestException;
import ru.orderhub.order_management.common.exception.NotFoundException;
import ru.orderhub.order_management.product.dto.ProductRequest;
import ru.orderhub.order_management.product.dto.ProductResponse;
import ru.orderhub.order_management.product.entity.Category;
import ru.orderhub.order_management.product.entity.Product;
import ru.orderhub.order_management.product.repository.CategoryRepository;
import ru.orderhub.order_management.product.repository.ProductRepository;

@Service
public class ProductService {

    private static final int MAX_PAGE_SIZE = 50;

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    /**
     * Список товаров с поиском по названию и пагинацией.
     * Показываем только активные товары.
     */
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getAll(String search, int page, int size) {
        validatePaging(page, size);

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("id").ascending());

        String normalizedSearch = (search == null || search.isBlank()) ? null : search.trim();

        Page<Product> products = normalizedSearch == null
                ? productRepository.findByActiveTrue(pageRequest)
                : productRepository.findByNameContainingIgnoreCaseAndActiveTrue(normalizedSearch, pageRequest);

        return PageResponse.from(products, ProductResponse::from);
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        return ProductResponse.from(findProduct(id));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product = new Product();
        applyRequest(product, request);
        product.setActive(true);

        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = findProduct(id);
        applyRequest(product, request);

        // Менять сущность под @Transactional достаточно, save() не обязателен.
        return ProductResponse.from(product);
    }

    /**
     * Мягкое удаление: товар скрывается из каталога, но остаётся в старых заказах.
     * Так мы не ломаем внешние ключи у order_items.
     */
    @Transactional
    public void delete(Long id) {
        Product product = findProduct(id);
        product.setActive(false);
    }

    private Product findProduct(Long id) {
        return productRepository.findByIdWithCategory(id)
                .orElseThrow(() -> new NotFoundException("Товар не найден: " + id));
    }

    private void applyRequest(Product product, ProductRequest request) {
        product.setName(request.name().trim());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStockQuantity(request.stockQuantity());

        if (request.categoryId() == null) {
            product.setCategory(null);
            return;
        }

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new NotFoundException("Категория не найдена: " + request.categoryId()));
        product.setCategory(category);
    }

    private void validatePaging(int page, int size) {
        if (page < 0) {
            throw new BadRequestException("Номер страницы не может быть отрицательным");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("Размер страницы должен быть от 1 до " + MAX_PAGE_SIZE);
        }
    }
}
