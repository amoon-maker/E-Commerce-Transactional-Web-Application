package com.umhamza.ecommerce.repository;

import com.umhamza.ecommerce.entity.Order;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository
        extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByCustomer_CustomerId(Long customerId);

    boolean existsByOrderNumber(String orderNumber);
}