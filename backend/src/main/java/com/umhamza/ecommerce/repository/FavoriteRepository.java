package com.umhamza.ecommerce.repository;

import com.umhamza.ecommerce.entity.Favorite;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    @EntityGraph(attributePaths = "product")
    List<Favorite> findByCustomerCustomerIdOrderByCreatedAtDesc(Long customerId);

    Optional<Favorite> findByCustomerCustomerIdAndProductProductId(Long customerId, Long productId);

    boolean existsByCustomerCustomerIdAndProductProductId(Long customerId, Long productId);

    void deleteByCustomerCustomerIdAndProductProductId(Long customerId, Long productId);
}
