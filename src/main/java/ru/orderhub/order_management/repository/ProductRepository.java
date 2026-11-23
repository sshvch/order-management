package ru.orderhub.order_management.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.orderhub.order_management.entity.Product;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * EntityGraph подгружает категорию сразу, чтобы не делать отдельный запрос
     * на каждый товар в списке (проблема N+1).
     */
    @EntityGraph(attributePaths = "category")
    Page<Product> findByActiveTrue(Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Page<Product> findByNameContainingIgnoreCaseAndActiveTrue(String name, Pageable pageable);

    @EntityGraph(attributePaths = "category")
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdWithCategory(@Param("id") Long id);

    boolean existsByCategoryId(Long categoryId);

    /**
     * Уменьшает остаток одним SQL-запросом.
     *
     * Условие "stock_quantity >= :quantity" проверяется прямо в базе, поэтому два
     * одновременных заказа не смогут списать один и тот же товар дважды:
     * второй запрос просто обновит 0 строк.
     *
     * @return количество обновлённых строк (1 — успешно, 0 — товара не хватило)
     */
    @Modifying
    @Query("""
            update Product p
            set p.stockQuantity = p.stockQuantity - :quantity
            where p.id = :id
              and p.active = true
              and p.stockQuantity >= :quantity
            """)
    int decreaseStock(@Param("id") Long id, @Param("quantity") int quantity);

    /**
     * Возвращает товар на склад при отмене заказа.
     */
    @Modifying
    @Query("update Product p set p.stockQuantity = p.stockQuantity + :quantity where p.id = :id")
    int increaseStock(@Param("id") Long id, @Param("quantity") int quantity);

    /**
     * Актуальный остаток нужен только для текста ошибки, когда списать не удалось.
     */
    @Query("select p.stockQuantity from Product p where p.id = :id")
    Integer findStockQuantityById(@Param("id") Long id);
}
