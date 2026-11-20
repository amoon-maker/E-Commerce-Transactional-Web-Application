package com.umhamza.ecommerce.service;

import com.umhamza.ecommerce.dto.FavoriteResponse;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface FavoriteService {
    List<FavoriteResponse> getFavorites(Authentication authentication);

    FavoriteResponse addFavorite(Authentication authentication, Long productId);

    void removeFavorite(Authentication authentication, Long productId);
}
