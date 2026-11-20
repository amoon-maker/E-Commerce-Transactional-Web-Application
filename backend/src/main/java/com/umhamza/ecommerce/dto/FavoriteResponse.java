package com.umhamza.ecommerce.dto;

import java.math.BigDecimal;

public record FavoriteResponse(
        Long productId,
        String sku,
        String productName,
        BigDecimal unitPrice,
        Integer availableStock,
        boolean active
) {}
