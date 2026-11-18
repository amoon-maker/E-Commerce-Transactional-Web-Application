
package com.umhamza.ecommerce.repository;

import com.umhamza.ecommerce.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductVariantRepository
        extends JpaRepository<ProductVariant, Long> {

    // Retrieve active variants for customers.
    List<ProductVariant> findByProduct_ProductIdAndActiveTrue(
            Long productId
    );

    // Retrieve all variants, including inactive ones.
    List<ProductVariant> findByProduct_ProductId(
            Long productId
    );

    // Check whether a variant SKU already exists.
    boolean existsBySkuIgnoreCase(String sku);

    // Check for duplicate SKU when updating a variant.
    boolean existsBySkuIgnoreCaseAndVariantIdNot(
            String sku,
            Long variantId
    );
}
