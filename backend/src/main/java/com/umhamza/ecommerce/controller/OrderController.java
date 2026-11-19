package com.umhamza.ecommerce.controller;

import com.umhamza.ecommerce.dto.OrderResponse;
import com.umhamza.ecommerce.entity.Customer;
import com.umhamza.ecommerce.entity.User;
import com.umhamza.ecommerce.repository.CustomerRepository;
import com.umhamza.ecommerce.repository.UserRepository;
import com.umhamza.ecommerce.service.OrderService;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.umhamza.ecommerce.dto.OrderRequest;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;

    public OrderController(
            OrderService orderService,
            UserRepository userRepository,
            CustomerRepository customerRepository) {

        this.orderService = orderService;
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
    }

    @GetMapping("/my-orders")
    public List<OrderResponse> getMyOrders(Authentication authentication) {

        User user = userRepository.findByEmailIgnoreCase(
                        authentication.getName())
                .orElseThrow(() -> new EntityNotFoundException(
                        "User not found."
                ));

        Customer customer = customerRepository
                .findByUserUserId(user.getUserId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Customer profile not found."
                ));

        return orderService.getOrdersByCustomerId(
                customer.getCustomerId()
        );
    }
        @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody OrderRequest request,
            Authentication authentication
    ) {

        User user = userRepository.findByEmailIgnoreCase(
                        authentication.getName())
                .orElseThrow(() -> new EntityNotFoundException(
                        "User not found."
                ));

        Customer customer = customerRepository
                .findByUserUserId(user.getUserId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Customer profile not found."
                ));

        OrderResponse response = orderService.createOrder(
                customer.getCustomerId(),
                request
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}