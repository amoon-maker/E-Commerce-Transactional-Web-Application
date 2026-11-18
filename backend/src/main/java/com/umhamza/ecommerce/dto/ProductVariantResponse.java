
package com.umhamza.ecommerce.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductVariantResponse(

        Long variantId,
        String sku,
        String variantName,
        BigDecimal packageSize,
        String packageUnit,
        BigDecimal price,
        BigDecimal salePrice,
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
