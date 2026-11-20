package com.umhamza.ecommerce.repository;

import com.umhamza.ecommerce.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findByCustomer_CustomerId(Long customerId);

}