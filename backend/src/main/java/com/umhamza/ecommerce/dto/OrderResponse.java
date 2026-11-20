package com.umhamza.ecommerce.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderResponse(
        Long orderId,
        String orderNumber,
        LocalDateTime orderDate,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal taxAmount,
        BigDecimal shippingCost,
        BigDecimal totalAmount,
        String status,
        String invoiceNumber
) {
}
