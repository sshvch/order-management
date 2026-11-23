package ru.orderhub.order_management.cart.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.orderhub.order_management.cart.dto.AddCartItemRequest;
import ru.orderhub.order_management.cart.dto.CartResponse;
import ru.orderhub.order_management.cart.dto.UpdateCartItemRequest;
import ru.orderhub.order_management.cart.entity.Cart;
import ru.orderhub.order_management.cart.entity.CartItem;
import ru.orderhub.order_management.cart.repository.CartItemRepository;
import ru.orderhub.order_management.cart.repository.CartRepository;
import ru.orderhub.order_management.common.exception.BadRequestException;
import ru.orderhub.order_management.common.exception.NotFoundException;
import ru.orderhub.order_management.product.entity.Product;
import ru.orderhub.order_management.product.repository.ProductRepository;
import ru.orderhub.order_management.user.entity.User;
import ru.orderhub.order_management.user.repository.UserRepository;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public CartService(CartRepository cartRepository,
                       CartItemRepository cartItemRepository,
                       ProductRepository productRepository,
                       UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CartResponse getCart(String email) {
        return CartResponse.from(findOrCreateCart(email));
    }

    /**
     * Если такой товар уже лежит в корзине — увеличиваем количество.
     */
    @Transactional
    public CartResponse addItem(String email, AddCartItemRequest request) {
        Cart cart = findOrCreateCart(email);

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new NotFoundException("Товар не найден: " + request.productId()));

        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new BadRequestException("Этот товар больше не продаётся");
        }

        CartItem existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(product.getId()))
                .findFirst()
                .orElse(null);

        int newQuantity = request.quantity() + (existingItem != null ? existingItem.getQuantity() : 0);
        checkStock(product, newQuantity);

        if (existingItem != null) {
            existingItem.setQuantity(newQuantity);
        } else {
            cart.addItem(new CartItem(product, request.quantity()));
        }

        cartRepository.save(cart);
        return CartResponse.from(cart);
    }

    @Transactional
    public CartResponse updateItem(String email, Long itemId, UpdateCartItemRequest request) {
        CartItem item = cartItemRepository.findByIdAndCartUserEmail(itemId, email)
                .orElseThrow(() -> new NotFoundException("Позиция корзины не найдена: " + itemId));

        checkStock(item.getProduct(), request.quantity());
        item.setQuantity(request.quantity());

        return CartResponse.from(item.getCart());
    }

    @Transactional
    public CartResponse removeItem(String email, Long itemId) {
        CartItem item = cartItemRepository.findByIdAndCartUserEmail(itemId, email)
                .orElseThrow(() -> new NotFoundException("Позиция корзины не найдена: " + itemId));

        Cart cart = item.getCart();
        // orphanRemoval удалит позицию из базы, когда Hibernate сделает flush.
        cart.getItems().remove(item);

        return CartResponse.from(cart);
    }

    @Transactional
    public CartResponse clear(String email) {
        Cart cart = findOrCreateCart(email);
        cart.clearItems();
        cartRepository.save(cart);

        return CartResponse.from(cart);
    }

    private Cart findOrCreateCart(String email) {
        return cartRepository.findByUserEmail(email)
                .orElseGet(() -> createCart(email));
    }

    private Cart createCart(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден: " + email));

        Cart cart = new Cart();
        cart.setUser(user);
        return cartRepository.save(cart);
    }

    private void checkStock(Product product, int quantity) {
        if (quantity > product.getStockQuantity()) {
            throw new BadRequestException(
                    "Недостаточно товара \"" + product.getName() + "\". На складе: " + product.getStockQuantity()
            );
        }
    }
}
