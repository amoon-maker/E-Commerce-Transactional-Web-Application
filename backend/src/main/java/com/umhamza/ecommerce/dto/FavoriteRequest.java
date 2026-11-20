package com.umhamza.ecommerce.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record FavoriteRequest(@NotNull @Positive Long productId) {}
