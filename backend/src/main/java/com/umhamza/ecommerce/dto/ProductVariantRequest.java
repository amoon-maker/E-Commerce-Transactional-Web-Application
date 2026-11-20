
package com.umhamza.ecommerce.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductVariantRequest(

        Long variantId,

        @NotBlank(message = "Variant SKU is required")
        @Size(max = 120, message = "Variant SKU cannot exceed 120 characters")
        String sku,

        @NotBlank(message = "Variant name is required")
        @Size(max = 100, message = "Variant name cannot exceed 100 characters")
        String variantName,

        @DecimalMin(value = "0.00", inclusive = false,
                message = "Package size must be greater than zero")
        @Digits(integer = 7, fraction = 2,
                message = "Package size must have at most 7 integer digits and 2 decimal places")
        BigDecimal packageSize,

        @Size(max = 40, message = "Package unit cannot exceed 40 characters")
        String packageUnit,

        @NotNull(message = "Variant price is required")
        @DecimalMin(value = "0.00", message = "Variant price cannot be negative")
        @Digits(integer = 7, fraction = 2,
                message = "Variant price must have at most 7 integer digits and 2 decimal places")
        BigDecimal price,

        @DecimalMin(value = "0.00", message = "Sale price cannot be negative")
        @Digits(integer = 7, fraction = 2,
                message = "Sale price must have at most 7 integer digits and 2 decimal places")
        BigDecimal salePrice,

        @Min(value = 0, message = "Stock quantity cannot be negative")
        Integer stockQuantity,

        @Min(value = 0, message = "Reorder level cannot be negative")
        Integer reorderLevel,

        Boolean active
) {
}
