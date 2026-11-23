package ru.orderhub.order_management.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.orderhub.order_management.product.entity.Category;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByOrderByNameAsc();

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}
