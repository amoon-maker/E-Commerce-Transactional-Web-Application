
package com.umhamza.ecommerce.service;

import com.umhamza.ecommerce.entity.Customer;
import com.umhamza.ecommerce.entity.Role;
import com.umhamza.ecommerce.entity.User;
import com.umhamza.ecommerce.repository.CustomerRepository;
import com.umhamza.ecommerce.repository.RoleRepository;
import com.umhamza.ecommerce.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(
            UserRepository userRepository,
            CustomerRepository customerRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void register(
            String email,
            String rawPassword,
            String firstName,
            String lastName,
            String phone) {

        String normalizedEmail = email.trim();

        if (userRepository.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
            throw new IllegalArgumentException("Email is already registered.");
        }

        Role customerRole = roleRepository.findByRoleName("CUSTOMER")
                .orElseThrow(() ->
                        new IllegalStateException("CUSTOMER role does not exist."));

        User user = new User(
        normalizedEmail,
        passwordEncoder.encode(rawPassword),
        customerRole
);

        User savedUser = userRepository.save(user);

        Customer customer = new Customer(
                savedUser,
                firstName.trim(),
                lastName.trim(),
                phone
        );

        customerRepository.save(customer);
    }
}
