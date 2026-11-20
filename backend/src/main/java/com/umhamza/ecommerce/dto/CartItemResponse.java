package com.umhamza.ecommerce.dto;

import java.math.BigDecimal;

public record CartItemResponse(
        Long cartItemId,
        Long productId,
        Long vendorId,
        String sku,
        String productName,
        String description,
        BigDecimal unitPrice,
        Integer quantity,
        Integer availableStock,
        boolean active
) {}
