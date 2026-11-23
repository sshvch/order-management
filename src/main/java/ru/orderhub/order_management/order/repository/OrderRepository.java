package ru.orderhub.order_management.order.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.orderhub.order_management.order.entity.Order;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByUserEmail(String email, Pageable pageable);

    /**
     * Заказ вместе с позициями и товарами — для карточки заказа.
     * Для списка заказов позиции не нужны, поэтому там обычный запрос.
     */
    @Query("""
            select distinct o from Order o
            left join fetch o.items i
            left join fetch i.product
            where o.id = :id
            """)
    Optional<Order> findByIdWithItems(@Param("id") Long id);
}
