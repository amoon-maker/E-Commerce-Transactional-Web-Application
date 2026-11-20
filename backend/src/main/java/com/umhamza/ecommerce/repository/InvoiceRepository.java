package com.umhamza.ecommerce.repository;

import com.umhamza.ecommerce.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByOrder_OrderId(Long orderId);

    List<Invoice> findByOrder_OrderIdIn(List<Long> orderIds);
}
