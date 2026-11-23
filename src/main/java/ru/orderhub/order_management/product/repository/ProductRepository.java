package ru.orderhub.order_management.product.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.orderhub.order_management.product.entity.Product;

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
}
