
package com.umhamza.ecommerce.repository;

import com.umhamza.ecommerce.entity.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VendorRepository extends JpaRepository<Vendor, Long> {

    boolean existsByBusinessNameIgnoreCase(String businessName);

    Optional<Vendor> findByBusinessNameIgnoreCase(String businessName);

    boolean existsByBusinessNameIgnoreCaseAndVendorIdNot(
        String businessName,
        Long vendorId
);
}
