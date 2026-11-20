package com.umhamza.ecommerce.service;

import com.umhamza.ecommerce.dto.CartResponse;
import org.springframework.security.core.Authentication;

public interface CartService {
    CartResponse getCart(Authentication authentication);

    CartResponse addItem(Authentication authentication, Long productId, Integer quantity);

    CartResponse updateQuantity(
            Authentication authentication, Long productId, Integer quantity);

    CartResponse removeItem(Authentication authentication, Long productId);

    CartResponse clearCart(Authentication authentication);
}
