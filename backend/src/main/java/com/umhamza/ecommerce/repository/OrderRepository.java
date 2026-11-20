package com.umhamza.ecommerce.repository;

import com.umhamza.ecommerce.entity.Order;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository
        extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByCustomer_CustomerId(Long customerId);

    Optional<Order> findByOrderIdAndCustomer_CustomerId(
            Long orderId,
            Long customerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.orderId = :orderId")
    Optional<Order> findByOrderIdForUpdate(@Param("orderId") Long orderId);

    boolean existsByOrderNumber(String orderNumber);
}
