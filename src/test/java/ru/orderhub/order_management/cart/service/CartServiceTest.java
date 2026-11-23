package ru.orderhub.order_management.cart.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.orderhub.order_management.cart.dto.AddCartItemRequest;
import ru.orderhub.order_management.cart.dto.CartResponse;
import ru.orderhub.order_management.cart.entity.Cart;
import ru.orderhub.order_management.cart.entity.CartItem;
import ru.orderhub.order_management.cart.repository.CartItemRepository;
import ru.orderhub.order_management.cart.repository.CartRepository;
import ru.orderhub.order_management.common.exception.BadRequestException;
import ru.orderhub.order_management.product.entity.Product;
import ru.orderhub.order_management.product.repository.ProductRepository;
import ru.orderhub.order_management.user.entity.User;
import ru.orderhub.order_management.user.repository.UserRepository;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CartService cartService;

    private User user;
    private Cart cart;
    private Product product;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("user@orderhub.local");

        cart = new Cart();
        cart.setId(1L);
        cart.setUser(user);

        product = new Product();
        product.setId(10L);
        product.setName("Наушники");
        product.setPrice(new BigDecimal("8990.00"));
        product.setStockQuantity(5);
        product.setActive(true);
    }

    @Test
    void addItemPutsProductIntoCart() {
        when(cartRepository.findByUserEmail("user@orderhub.local")).thenReturn(Optional.of(cart));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartResponse response = cartService.addItem(
                "user@orderhub.local",
                new AddCartItemRequest(10L, 2)
        );

        assertThat(response.items()).hasSize(1);
        assertThat(response.totalItems()).isEqualTo(2);
        assertThat(response.totalPrice()).isEqualByComparingTo("17980.00");
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    void addItemThrowsWhenNotEnoughStock() {
        when(cartRepository.findByUserEmail("user@orderhub.local")).thenReturn(Optional.of(cart));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() ->
                cartService.addItem("user@orderhub.local", new AddCartItemRequest(10L, 10))
        )
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Недостаточно товара");

        assertThat(cart.getItems()).isEmpty();
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void updateItemChangesQuantity() {
        CartItem item = new CartItem(product, 1);
        item.setId(100L);
        cart.addItem(item);

        when(cartItemRepository.findByIdAndCartUserEmail(100L, "user@orderhub.local"))
                .thenReturn(Optional.of(item));

        CartResponse response = cartService.updateItem(
                "user@orderhub.local",
                100L,
                new ru.orderhub.order_management.cart.dto.UpdateCartItemRequest(4)
        );

        assertThat(item.getQuantity()).isEqualTo(4);
        assertThat(response.totalPrice()).isEqualByComparingTo("35960.00");
    }
}
