package ru.orderhub.order_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.orderhub.order_management.entity.CartItem;

import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByIdAndCartUserEmail(Long id, String email);
}
