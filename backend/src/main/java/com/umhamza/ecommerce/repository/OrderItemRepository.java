package com.umhamza.ecommerce.repository;

import com.umhamza.ecommerce.entity.OrderItem;
import com.umhamza.ecommerce.entity.Order;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository
        extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrder_OrderId(Long orderId);

    List<OrderItem> findByProduct_ProductId(Long productId);

    @Query("SELECT DISTINCT oi.order FROM OrderItem oi WHERE oi.product.vendor.vendorId = :vendorId ORDER BY oi.order.orderDate DESC")
    List<Order> findOrdersByVendorId(@Param("vendorId") Long vendorId);
}
