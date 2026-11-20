package com.umhamza.ecommerce.controller;

import com.umhamza.ecommerce.dto.CartItemRequest;
import com.umhamza.ecommerce.dto.CartQuantityRequest;
import com.umhamza.ecommerce.dto.CartResponse;
import com.umhamza.ecommerce.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartResponse getCart(Authentication authentication) {
        return cartService.getCart(authentication);
    }

    @PostMapping("/items")
    public CartResponse addItem(
            @Valid @RequestBody CartItemRequest request,
            Authentication authentication) {
        return cartService.addItem(authentication, request.productId(), request.quantity());
    }

    @PutMapping("/items/{productId}")
    public CartResponse updateQuantity(
            @PathVariable Long productId,
            @Valid @RequestBody CartQuantityRequest request,
            Authentication authentication) {
        return cartService.updateQuantity(authentication, productId, request.quantity());
    }

    @DeleteMapping("/items/{productId}")
    public CartResponse removeItem(
            @PathVariable Long productId,
            Authentication authentication) {
        return cartService.removeItem(authentication, productId);
    }

    @DeleteMapping
    public CartResponse clearCart(Authentication authentication) {
        return cartService.clearCart(authentication);
    }
}
