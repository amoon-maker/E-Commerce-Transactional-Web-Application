package com.umhamza.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OrderRejectionRequest(
        @NotBlank(message = "A rejection reason is required")
        @Size(max = 500, message = "Rejection reason must not exceed 500 characters")
        String reason
) {}
