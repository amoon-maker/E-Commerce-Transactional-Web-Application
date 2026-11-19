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
}