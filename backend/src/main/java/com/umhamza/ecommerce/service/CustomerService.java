package com.umhamza.ecommerce.service;

import com.umhamza.ecommerce.dto.CustomerResponse;
import com.umhamza.ecommerce.entity.Customer;
import com.umhamza.ecommerce.repository.CustomerRepository;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public CustomerResponse getCustomerByUserId(Long userId) {

        Customer customer = customerRepository.findByUserUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Customer not found for user ID: " + userId
                ));

        return toResponse(customer);
    }

    private CustomerResponse toResponse(Customer customer) {

        return new CustomerResponse(
                customer.getCustomerId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getUser().getEmail(),
                customer.getPhone(),
                customer.getCreatedAt(),
                customer.getUpdatedAt()
        );
    }
}