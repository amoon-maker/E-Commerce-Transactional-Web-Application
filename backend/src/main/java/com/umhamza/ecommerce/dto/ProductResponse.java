package com.umhamza.ecommerce.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(
        Long productId,
        Long vendorId,
        String sku,
        String productName,
        String description,
        BigDecimal price,
        BigDecimal salePrice,
        BigDecimal packageSize,
        String packageUnit,
        Integer stockQuantity,
        Integer reservedQuantity,
        Integer reorderLevel,
        String status,
        boolean active,
        long versionNo,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}