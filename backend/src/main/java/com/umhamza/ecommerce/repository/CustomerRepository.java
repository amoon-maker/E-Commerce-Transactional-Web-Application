
package com.umhamza.ecommerce.repository;

import com.umhamza.ecommerce.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByUserUserId(Long userId);

    boolean existsByUserUserId(Long userId);
}
