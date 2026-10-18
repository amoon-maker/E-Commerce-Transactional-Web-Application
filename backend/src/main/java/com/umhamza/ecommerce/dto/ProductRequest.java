package com.umhamza.ecommerce.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductRequest(

        @NotNull(message = "Vendor ID is required")
        @Positive(message = "Vendor ID must be positive")
        Long vendorId,

        @NotBlank(message = "SKU is required")
        @Size(max = 60, message = "SKU cannot exceed 60 characters")
        String sku,

        @NotBlank(message = "Product name is required")
        @Size(max = 180, message = "Product name cannot exceed 180 characters")
        String productName,

        String description,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.00", message = "Price cannot be negative")
        @Digits(integer = 10, fraction = 2,
                message = "Price must have at most 10 integer digits and 2 decimal places")
        BigDecimal price,

        @DecimalMin(value = "0.00", message = "Sale price cannot be negative")
        @Digits(integer = 10, fraction = 2,
                message = "Sale price must have at most 10 integer digits and 2 decimal places")
        BigDecimal salePrice,

        @DecimalMin(value = "0.00", inclusive = false,
                message = "Package size must be greater than zero")
        @Digits(integer = 8, fraction = 2,
                message = "Package size must have at most 8 integer digits and 2 decimal places")
        BigDecimal packageSize,

        @Size(max = 20, message = "Package unit cannot exceed 20 characters")
        String packageUnit,

        @Min(value = 0, message = "Stock quantity cannot be negative")
        Integer stockQuantity,

        @Min(value = 0, message = "Reorder level cannot be negative")
        Integer reorderLevel,

        Boolean active
) {
}