package ru.orderhub.order_management.product.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.orderhub.order_management.common.exception.BadRequestException;
import ru.orderhub.order_management.common.exception.NotFoundException;
import ru.orderhub.order_management.product.dto.CategoryRequest;
import ru.orderhub.order_management.product.dto.CategoryResponse;
import ru.orderhub.order_management.product.entity.Category;
import ru.orderhub.order_management.product.repository.CategoryRepository;
import ru.orderhub.order_management.product.repository.ProductRepository;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAll() {
        return categoryRepository.findAllByOrderByNameAsc().stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String name = request.name().trim();

        if (categoryRepository.existsByName(name)) {
            throw new BadRequestException("Категория с таким названием уже существует");
        }

        Category category = new Category();
        category.setName(name);
        category.setDescription(request.description());

        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = findCategory(id);
        String name = request.name().trim();

        if (categoryRepository.existsByNameAndIdNot(name, id)) {
            throw new BadRequestException("Категория с таким названием уже существует");
        }

        category.setName(name);
        category.setDescription(request.description());

        return CategoryResponse.from(category);
    }

    @Transactional
    public void delete(Long id) {
        Category category = findCategory(id);

        // Не даём удалить категорию, на которую ссылаются товары.
        if (productRepository.existsByCategoryId(id)) {
            throw new BadRequestException("Нельзя удалить категорию, в которой есть товары");
        }

        categoryRepository.delete(category);
    }

    private Category findCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Категория не найдена: " + id));
    }
}
