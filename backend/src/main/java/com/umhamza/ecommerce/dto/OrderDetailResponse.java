package com.umhamza.ecommerce.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record OrderDetailResponse(
        Long orderId,
        String orderNumber,
        LocalDateTime orderDate,
        String status,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal taxAmount,
        BigDecimal shippingCost,
        BigDecimal totalAmount,
        AddressSummary deliveryAddress,
        List<ItemSummary> items,
        InvoiceSummary invoice,
        PaymentSummary payment,
        DeliverySummary delivery,
        List<StatusHistoryEntry> statusHistory
) {
    public OrderDetailResponse {
        items = List.copyOf(items);
        statusHistory = List.copyOf(statusHistory);
    }

    public record AddressSummary(
            String addressLabel,
            String addressLine1,
            String addressLine2,
            String city,
            String emirate,
            String postalCode,
            String country,
            String addressType
    ) {}

    public record ItemSummary(
            Long productId,
            String sku,
            String productName,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal
    ) {}

    public record InvoiceSummary(
            String invoiceNumber,
            LocalDate issueDate,
            BigDecimal subtotal,
            BigDecimal discountAmount,
            BigDecimal taxAmount,
            BigDecimal shippingCost,
            BigDecimal totalAmount,
            String status
    ) {}

    public record PaymentSummary(
            String paymentMethod,
            String status,
            BigDecimal authorizedAmount,
            BigDecimal capturedAmount,
            String currencyCode
    ) {}

    public record DeliverySummary(
            String status,
            String deliveryMethod,
            LocalDate estimatedDeliveryDate,
            LocalDate actualDeliveryDate,
            String notes
    ) {}

    public record StatusHistoryEntry(
            String status,
            String comment,
            LocalDateTime changedAt
    ) {}
}
