package com.umhamza.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddressRequest(

        @Size(max = 50, message = "Address label must not exceed 50 characters")
        String addressLabel,

        @NotBlank(message = "Address line 1 is required")
        @Size(max = 255, message = "Address line 1 must not exceed 255 characters")
        String addressLine1,

        @Size(max = 255, message = "Address line 2 must not exceed 255 characters")
        String addressLine2,

        @NotBlank(message = "City is required")
        @Size(max = 100, message = "City must not exceed 100 characters")
        String city,

        @NotBlank(message = "Emirate is required")
        @Size(max = 100, message = "Emirate must not exceed 100 characters")
        String emirate,

        @Size(max = 20, message = "Postal code must not exceed 20 characters")
        String postalCode,

        @NotBlank(message = "Country is required")
        @Size(max = 100, message = "Country must not exceed 100 characters")
        String country,

        @NotBlank(message = "Address type is required")
        String addressType

) {
}