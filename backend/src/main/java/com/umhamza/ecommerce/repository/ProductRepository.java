package com.umhamza.ecommerce.repository;

import com.umhamza.ecommerce.entity.Product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository
        extends JpaRepository<Product, Long> {

    boolean existsBySkuIgnoreCase(String sku);

    Optional<Product> findBySkuIgnoreCase(String sku);

    List<Product> findByVendor_VendorId(Long vendorId);

    List<Product> findByActiveTrue();

    boolean existsBySkuIgnoreCaseAndProductIdNot(
        String sku,
        Long productId
);
}