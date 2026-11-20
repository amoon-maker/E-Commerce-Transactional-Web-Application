package com.umhamza.ecommerce.dto;

import java.util.List;

public record CartResponse(List<CartItemResponse> items) {
    public CartResponse {
        items = List.copyOf(items);
    }
}
