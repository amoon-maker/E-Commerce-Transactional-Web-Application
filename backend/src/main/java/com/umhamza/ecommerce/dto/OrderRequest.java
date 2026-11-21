package com.umhamza.ecommerce.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record OrderRequest(

        @NotNull(message = "Delivery address is required")
        @Valid
        AddressRequest deliveryAddress,

        @NotNull(message = "Payment details are required")
        @Valid
        PaymentRequest payment

) {
}
