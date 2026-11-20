package com.umhamza.ecommerce.service;

import com.umhamza.ecommerce.entity.Customer;
import com.umhamza.ecommerce.entity.User;
import com.umhamza.ecommerce.repository.CustomerRepository;
import com.umhamza.ecommerce.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class CustomerAccountService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;

    public CustomerAccountService(
            UserRepository userRepository,
            CustomerRepository customerRepository) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
    }

    public Customer requireCustomer(Authentication authentication, boolean lockForUpdate) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Customer authentication is required.");
        }

        User user = userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new EntityNotFoundException("User account not found."));

        if (!user.isActive() || user.getRole() == null
                || !"CUSTOMER".equals(user.getRole().getRoleName())) {
            throw new AccessDeniedException("A customer account is required.");
        }

        return (lockForUpdate
                ? customerRepository.findByUserUserIdForUpdate(user.getUserId())
                : customerRepository.findByUserUserId(user.getUserId()))
                .orElseThrow(() -> new EntityNotFoundException("Customer profile not found."));
    }
}
