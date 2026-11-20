package com.umhamza.ecommerce.controller;

import com.umhamza.ecommerce.dto.DeliveryStatusRequest;
import com.umhamza.ecommerce.dto.OrderDetailResponse;
import com.umhamza.ecommerce.dto.OrderRejectionRequest;
import com.umhamza.ecommerce.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/vendor/orders")
public class VendorOrderController {

    private final OrderService orderService;

    public VendorOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<OrderDetailResponse> getVendorOrders(Authentication authentication) {
        return orderService.getVendorOrders(authentication);
    }

    @PutMapping("/{orderId}/approve")
    public OrderDetailResponse approveOrder(
            @PathVariable Long orderId,
            Authentication authentication) {
        return orderService.approveOrder(orderId, authentication);
    }

    @PutMapping("/{orderId}/reject")
    public OrderDetailResponse rejectOrder(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderRejectionRequest request,
            Authentication authentication) {
        return orderService.rejectOrder(orderId, request.reason(), authentication);
    }

    @PutMapping("/{orderId}/delivery-status")
    public OrderDetailResponse updateDeliveryStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody DeliveryStatusRequest request,
            Authentication authentication) {
        return orderService.updateDeliveryStatus(orderId, request.status(), authentication);
    }
}
