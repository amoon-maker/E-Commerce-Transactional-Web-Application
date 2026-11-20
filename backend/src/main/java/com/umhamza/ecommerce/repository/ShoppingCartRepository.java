package com.umhamza.ecommerce.repository;

import com.umhamza.ecommerce.entity.ShoppingCart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShoppingCartRepository extends JpaRepository<ShoppingCart, Long> {
    Optional<ShoppingCart> findByCustomerCustomerIdAndStatus(Long customerId, String status);
}
