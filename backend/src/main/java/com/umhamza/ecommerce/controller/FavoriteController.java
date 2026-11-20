package com.umhamza.ecommerce.controller;

import com.umhamza.ecommerce.dto.FavoriteRequest;
import com.umhamza.ecommerce.dto.FavoriteResponse;
import com.umhamza.ecommerce.service.FavoriteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping
    public List<FavoriteResponse> getFavorites(Authentication authentication) {
        return favoriteService.getFavorites(authentication);
    }

    @PostMapping
    public FavoriteResponse addFavorite(
            @Valid @RequestBody FavoriteRequest request,
            Authentication authentication) {
        return favoriteService.addFavorite(authentication, request.productId());
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> removeFavorite(
            @PathVariable Long productId,
            Authentication authentication) {
        favoriteService.removeFavorite(authentication, productId);
        return ResponseEntity.noContent().build();
    }
}
