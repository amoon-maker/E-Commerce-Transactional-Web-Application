package com.umhamza.ecommerce.repository;

import com.umhamza.ecommerce.entity.CartItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    @EntityGraph(attributePaths = "product")
    List<CartItem> findByCartCartIdOrderByAddedAtAsc(Long cartId);

    @EntityGraph(attributePaths = "product")
    Optional<CartItem> findByCartCartIdAndProductProductId(Long cartId, Long productId);

    void deleteByCartCartIdAndProductProductId(Long cartId, Long productId);

    void deleteByCartCartId(Long cartId);
}
