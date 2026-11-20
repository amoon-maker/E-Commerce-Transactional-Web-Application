package com.umhamza.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record DeliveryStatusRequest(
        @NotBlank(message = "Delivery status is required")
        @Pattern(regexp = "READY_FOR_DELIVERY|OUT_FOR_DELIVERY|DELIVERED",
                message = "Delivery status must be READY_FOR_DELIVERY, OUT_FOR_DELIVERY, or DELIVERED")
        String status
) {}
