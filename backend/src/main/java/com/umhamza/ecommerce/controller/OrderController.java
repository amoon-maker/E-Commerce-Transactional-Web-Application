package com.umhamza.ecommerce.controller;

import com.umhamza.ecommerce.dto.OrderDetailResponse;
import com.umhamza.ecommerce.dto.OrderRequest;
import com.umhamza.ecommerce.dto.OrderResponse;
import com.umhamza.ecommerce.dto.PaymentRequest;
import com.umhamza.ecommerce.entity.Customer;
import com.umhamza.ecommerce.service.CustomerAccountService;
import com.umhamza.ecommerce.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final CustomerAccountService customerAccountService;

    public OrderController(
            OrderService orderService,
            CustomerAccountService customerAccountService) {
        this.orderService = orderService;
        this.customerAccountService = customerAccountService;
    }

    @GetMapping("/my-orders")
    public List<OrderResponse> getMyOrders(Authentication authentication) {
        Customer customer = customerAccountService.requireCustomer(authentication, false);
        return orderService.getOrdersByCustomerId(customer.getCustomerId());
    }

    @GetMapping("/my-orders/{orderId}")
    public OrderDetailResponse getMyOrderDetails(
            @PathVariable Long orderId,
            Authentication authentication) {
        Customer customer = customerAccountService.requireCustomer(authentication, false);
        return orderService.getOrderDetailsByCustomerId(orderId, customer.getCustomerId());
    }

    @PostMapping
    public ResponseEntity<OrderDetailResponse> createOrder(
            @Valid @RequestBody OrderRequest request,
            Authentication authentication) {
        OrderDetailResponse response = orderService.createOrder(authentication, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/my-orders/{orderId}/payment")
    public OrderDetailResponse capturePayment(
            @PathVariable Long orderId,
            @Valid @RequestBody PaymentRequest request,
            Authentication authentication) {
        return orderService.capturePayment(orderId, authentication, request);
    }

    @PostMapping("/my-orders/{orderId}/cancel")
    public OrderDetailResponse cancelOrder(
            @PathVariable Long orderId,
            Authentication authentication) {
        return orderService.cancelOrder(orderId, authentication);
    }
}
