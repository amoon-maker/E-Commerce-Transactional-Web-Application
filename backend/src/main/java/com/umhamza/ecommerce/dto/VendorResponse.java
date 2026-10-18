package com.umhamza.ecommerce.dto;

import java.time.LocalDateTime;

public record VendorResponse(
        Long vendorId,
        String businessName,
        String contactName,
        String email,
        String phone,
        String addressLine1,
        String city,
        String emirate,
        String country,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}