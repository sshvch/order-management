package ru.orderhub.order_management.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.orderhub.order_management.exception.NotFoundException;
import ru.orderhub.order_management.dto.ProductRequest;
import ru.orderhub.order_management.dto.ProductResponse;
import ru.orderhub.order_management.entity.Category;
import ru.orderhub.order_management.entity.Product;
import ru.orderhub.order_management.repository.CategoryRepository;
import ru.orderhub.order_management.repository.ProductRepository;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void getByIdThrowsWhenProductDoesNotExist() {
        when(productRepository.findByIdWithCategory(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getById(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Товар не найден");
    }

    @Test
    void createSavesProductWithCategory() {
        Category category = new Category();
        category.setId(1L);
        category.setName("Книги");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductRequest request = new ProductRequest("Книга", "Описание", new BigDecimal("3200.00"), 15, 1L);

        ProductResponse response = productService.create(request);

        assertThat(response.name()).isEqualTo("Книга");
        assertThat(response.price()).isEqualByComparingTo("3200.00");
        assertThat(response.categoryName()).isEqualTo("Книги");

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertThat(captor.getValue().getActive()).isTrue();
    }

    @Test
    void deleteHidesProductInsteadOfRemovingIt() {
        Product product = new Product();
        product.setName("Наушники");
        product.setActive(true);

        when(productRepository.findByIdWithCategory(5L)).thenReturn(Optional.of(product));

        productService.delete(5L);

        assertThat(product.getActive()).isFalse();
    }
}
