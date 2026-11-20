package com.umhamza.ecommerce.repository;

import com.umhamza.ecommerce.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByOrder_OrderId(Long orderId);

    Optional<Payment> findFirstByOrder_OrderIdOrderByCreatedAtDesc(Long orderId);
}
