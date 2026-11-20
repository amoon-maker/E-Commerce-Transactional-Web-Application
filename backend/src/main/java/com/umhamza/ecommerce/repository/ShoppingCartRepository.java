package com.umhamza.ecommerce.repository;

import com.umhamza.ecommerce.entity.ShoppingCart;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShoppingCartRepository extends JpaRepository<ShoppingCart, Long> {
    Optional<ShoppingCart> findByCustomerCustomerIdAndStatus(Long customerId, String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ShoppingCart c WHERE c.customer.customerId = :customerId AND c.status = :status")
    Optional<ShoppingCart> findByCustomerAndStatusForUpdate(
            @Param("customerId") Long customerId,
            @Param("status") String status);
}
