package com.umhamza.ecommerce.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VendorRequest(

        @NotBlank(message = "Business name is required")
        @Size(max = 150, message = "Business name cannot exceed 150 characters")
        String businessName,

        @Size(max = 120, message = "Contact name cannot exceed 120 characters")
        String contactName,

        @Email(message = "Please provide a valid email address")
        @Size(max = 255, message = "Email cannot exceed 255 characters")
        String email,

        @Size(max = 30, message = "Phone cannot exceed 30 characters")
        String phone,

        @Size(max = 200, message = "Address cannot exceed 200 characters")
        String addressLine1,

        @Size(max = 80, message = "City cannot exceed 80 characters")
        String city,

        @Size(max = 80, message = "Emirate cannot exceed 80 characters")
        String emirate,

        @NotBlank(message = "Country is required")
        @Size(max = 80, message = "Country cannot exceed 80 characters")
        String country,

        Boolean active
) {
}