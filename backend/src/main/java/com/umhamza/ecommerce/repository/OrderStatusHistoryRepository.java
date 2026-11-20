package com.umhamza.ecommerce.repository;

import com.umhamza.ecommerce.entity.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {
    List<OrderStatusHistory> findByOrder_OrderIdOrderByChangedAtAscHistoryIdAsc(Long orderId);
}
