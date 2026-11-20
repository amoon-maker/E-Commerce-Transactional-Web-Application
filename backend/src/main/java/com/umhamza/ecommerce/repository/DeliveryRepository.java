package com.umhamza.ecommerce.repository;

import com.umhamza.ecommerce.entity.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    Optional<Delivery> findByOrder_OrderId(Long orderId);
}
