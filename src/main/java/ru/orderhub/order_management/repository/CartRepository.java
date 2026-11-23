package ru.orderhub.order_management.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.orderhub.order_management.entity.Cart;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    /**
     * Корзина сразу приходит вместе с позициями и товарами.
     */
    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Cart> findByUserEmail(String email);
}
