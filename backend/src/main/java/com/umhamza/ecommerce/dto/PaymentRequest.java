package com.umhamza.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PaymentRequest(
        @NotBlank(message = "Payment method is required")
        @Pattern(regexp = "CREDIT|DEBIT", message = "Payment method must be CREDIT or DEBIT")
        String paymentMethod
) {}
