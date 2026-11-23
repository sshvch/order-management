package ru.orderhub.order_management.cart.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.orderhub.order_management.cart.entity.CartItem;

import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    /**
     * Ищем позицию по id и email владельца корзины.
     * Так пользователь не сможет изменить чужую позицию, даже если угадает id.
     */
    Optional<CartItem> findByIdAndCartUserEmail(Long id, String email);
}
