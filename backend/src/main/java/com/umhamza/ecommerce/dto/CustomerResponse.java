package com.umhamza.ecommerce.dto;

import java.time.LocalDateTime;

public record CustomerResponse(
        Long customerId,
        String firstName,
        String lastName,
        String email,
        String phone,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}