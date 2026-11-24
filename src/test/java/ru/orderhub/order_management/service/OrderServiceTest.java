package ru.orderhub.order_management.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.orderhub.order_management.entity.Cart;
import ru.orderhub.order_management.entity.CartItem;
import ru.orderhub.order_management.repository.CartRepository;
import ru.orderhub.order_management.exception.BadRequestException;
import ru.orderhub.order_management.dto.OrderResponse;
import ru.orderhub.order_management.entity.Order;
import ru.orderhub.order_management.entity.OrderItem;
import ru.orderhub.order_management.entity.OrderStatus;
import ru.orderhub.order_management.repository.OrderRepository;
import ru.orderhub.order_management.entity.Product;
import ru.orderhub.order_management.repository.ProductRepository;
import ru.orderhub.order_management.entity.User;
import ru.orderhub.order_management.repository.UserRepository;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderService orderService;

    private User user;
    private Product product;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("user@orderhub.local");

        product = new Product();
        product.setId(10L);
        product.setName("Ноутбук");
        product.setPrice(new BigDecimal("100.00"));
        product.setStockQuantity(10);
        product.setActive(true);
    }

    @Test
    void createOrderCalculatesTotalAndDecreasesStock() {
        Cart cart = new Cart();
        cart.setUser(user);
        cart.addItem(new CartItem(product, 3));

        when(userRepository.findByEmail("user@orderhub.local")).thenReturn(Optional.of(user));
        when(cartRepository.findByUserEmail("user@orderhub.local")).thenReturn(Optional.of(cart));
        when(productRepository.decreaseStock(10L, 3)).thenReturn(1);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.createOrder("user@orderhub.local");

        assertThat(response.totalAmount()).isEqualByComparingTo("300.00");
        assertThat(response.status()).isEqualTo("CREATED");
        assertThat(response.items()).hasSize(1);

        verify(productRepository).decreaseStock(10L, 3);
        assertThat(cart.getItems()).isEmpty();

        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void createOrderThrowsWhenStockIsNotEnough() {
        Cart cart = new Cart();
        cart.setUser(user);
        cart.addItem(new CartItem(product, 50));

        when(userRepository.findByEmail("user@orderhub.local")).thenReturn(Optional.of(user));
        when(cartRepository.findByUserEmail("user@orderhub.local")).thenReturn(Optional.of(cart));
        when(productRepository.decreaseStock(10L, 50)).thenReturn(0);
        when(productRepository.findStockQuantityById(10L)).thenReturn(0);

        assertThatThrownBy(() -> orderService.createOrder("user@orderhub.local"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Недостаточно товара");

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void createOrderThrowsWhenCartIsEmpty() {
        Cart cart = new Cart();
        cart.setUser(user);

        when(userRepository.findByEmail("user@orderhub.local")).thenReturn(Optional.of(user));
        when(cartRepository.findByUserEmail("user@orderhub.local")).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> orderService.createOrder("user@orderhub.local"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Корзина пуста");
    }

    @Test
    void cancelOrderReturnsProductsToStock() {
        Order order = new Order(user);
        order.setId(1L);
        order.addItem(new OrderItem(product, product.getName(), product.getPrice(), 3));

        when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.updateStatus(1L, OrderStatus.CANCELLED);

        assertThat(response.status()).isEqualTo("CANCELLED");
        verify(productRepository).increaseStock(10L, 3);
    }

    @Test
    void cannotShipCancelledOrder() {
        Order order = new Order(user);
        order.setId(2L);
        order.setStatus(OrderStatus.CANCELLED);

        when(orderRepository.findByIdWithItems(2L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateStatus(2L, OrderStatus.SHIPPED))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Нельзя изменить статус");
    }
}
