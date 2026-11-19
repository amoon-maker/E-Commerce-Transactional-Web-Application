package com.umhamza.ecommerce.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OrderRequest(

        @NotNull(message = "Delivery address is required")
        @Valid
        AddressRequest deliveryAddress,

        @NotEmpty(message = "An order must contain at least one item")
        List<@Valid OrderItemRequest> items

) {
}