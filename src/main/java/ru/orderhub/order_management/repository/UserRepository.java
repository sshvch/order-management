package ru.orderhub.order_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.orderhub.order_management.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}