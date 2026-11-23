package ru.orderhub.order_management.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.orderhub.order_management.entity.Cart;
import ru.orderhub.order_management.entity.CartItem;
import ru.orderhub.order_management.repository.CartRepository;
import ru.orderhub.order_management.dto.PageResponse;
import ru.orderhub.order_management.exception.BadRequestException;
import ru.orderhub.order_management.exception.NotFoundException;
import ru.orderhub.order_management.dto.OrderResponse;
import ru.orderhub.order_management.dto.OrderSummaryResponse;
import ru.orderhub.order_management.entity.Order;
import ru.orderhub.order_management.entity.OrderItem;
import ru.orderhub.order_management.entity.OrderStatus;
import ru.orderhub.order_management.repository.OrderRepository;
import ru.orderhub.order_management.entity.Product;
import ru.orderhub.order_management.entity.User;
import ru.orderhub.order_management.repository.UserRepository;

import java.math.BigDecimal;

@Service
public class OrderService {

    private static final int MAX_PAGE_SIZE = 50;

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;

    public OrderService(OrderRepository orderRepository,
                        CartRepository cartRepository,
                        UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.userRepository = userRepository;
    }

    /**
     * Оформление заказа из корзины.
     *
     * Всё происходит в одной транзакции: создаём заказ, создаём позиции,
     * уменьшаем остатки и очищаем корзину. Если на любом шаге вылетит исключение,
     * Spring откатит всё — и остатки товаров тоже вернутся на место.
     */
    @Transactional
    public OrderResponse createOrder(String email) {
        User user = findUser(email);

        Cart cart = cartRepository.findByUserEmail(email)
                .orElseThrow(() -> new BadRequestException("Корзина пуста"));

        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Корзина пуста");
        }

        Order order = new Order(user);
        BigDecimal total = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();

            if (!Boolean.TRUE.equals(product.getActive())) {
                throw new BadRequestException("Товар \"" + product.getName() + "\" больше не продаётся");
            }

            if (cartItem.getQuantity() > product.getStockQuantity()) {
                throw new BadRequestException(
                        "Недостаточно товара \"" + product.getName() + "\". На складе: "
                                + product.getStockQuantity()
                );
            }

            // Меняем остаток прямо у сущности из базы — Hibernate сохранит это при commit.
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());

            OrderItem orderItem = new OrderItem(
                    product,
                    product.getName(),
                    product.getPrice(),
                    cartItem.getQuantity()
            );
            order.addItem(orderItem);
            total = total.add(orderItem.getLineTotal());
        }

        order.setTotalAmount(total);
        Order savedOrder = orderRepository.save(order);

        cart.clearItems();
        cartRepository.save(cart);

        return OrderResponse.from(savedOrder);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> getMyOrders(String email, int page, int size) {
        validatePaging(page, size);

        Page<Order> orders = orderRepository.findByUserEmail(
                email,
                PageRequest.of(page, size, Sort.by("id").descending())
        );

        return PageResponse.from(orders, OrderSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> getAllOrders(int page, int size) {
        validatePaging(page, size);

        Page<Order> orders = orderRepository.findAll(
                PageRequest.of(page, size, Sort.by("id").descending())
        );

        return PageResponse.from(orders, OrderSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long id, String email, boolean admin) {
        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new NotFoundException("Заказ не найден: " + id));

        // Обычный пользователь видит только свои заказы.
        if (!admin && !order.getUser().getEmail().equals(email)) {
            throw new AccessDeniedException("Нет доступа к этому заказу");
        }

        return OrderResponse.from(order);
    }

    /**
     * Смена статуса заказа (для админа).
     * Если заказ отменяют — возвращаем товары на склад.
     */
    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatus newStatus) {
        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new NotFoundException("Заказ не найден: " + id));

        if (order.getStatus() == newStatus) {
            return OrderResponse.from(order);
        }

        if (!isAllowedTransition(order.getStatus(), newStatus)) {
            throw new BadRequestException(
                    "Нельзя изменить статус заказа с " + order.getStatus() + " на " + newStatus
            );
        }

        if (newStatus == OrderStatus.CANCELLED) {
            restoreStock(order);
        }

        order.setStatus(newStatus);
        return OrderResponse.from(order);
    }

    private void restoreStock(Order order) {
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
        }
    }

    /**
     * Какие переходы статусов разрешены.
     */
    private boolean isAllowedTransition(OrderStatus from, OrderStatus to) {
        return switch (from) {
            case CREATED -> to == OrderStatus.PAID || to == OrderStatus.CANCELLED;
            case PAID -> to == OrderStatus.SHIPPED || to == OrderStatus.CANCELLED;
            case SHIPPED -> to == OrderStatus.DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден: " + email));
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
