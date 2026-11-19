package com.umhamza.ecommerce.service;

import com.umhamza.ecommerce.dto.OrderResponse;
import com.umhamza.ecommerce.entity.Order;
import com.umhamza.ecommerce.repository.OrderRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public List<OrderResponse> getOrdersByCustomerId(Long customerId) {

        return orderRepository.findByCustomer_CustomerId(customerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private OrderResponse toResponse(Order order) {

        return new OrderResponse(
                order.getOrderId(),
                order.getOrderNumber(),
                order.getOrderDate(),
                order.getSubtotal(),
                order.getDiscountAmount(),
                order.getTaxAmount(),
                order.getShippingCost(),
                order.getTotalAmount(),
                order.getStatus()
        );
    }
}