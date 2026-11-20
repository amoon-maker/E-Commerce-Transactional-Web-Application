package com.umhamza.ecommerce.controller;

import com.umhamza.ecommerce.dto.CustomerResponse;
import com.umhamza.ecommerce.entity.User;
import com.umhamza.ecommerce.repository.UserRepository;
import com.umhamza.ecommerce.service.CustomerService;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final UserRepository userRepository;

    public CustomerController(
            CustomerService customerService,
            UserRepository userRepository) {

        this.customerService = customerService;
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public CustomerResponse getMyProfile(Authentication authentication) {

        User user = userRepository.findByEmailIgnoreCase(
                        authentication.getName())
                .orElseThrow(() -> new EntityNotFoundException(
                        "User not found."
                ));

        return customerService.getCustomerByUserId(user.getUserId());
    }
}