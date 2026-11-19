package com.umhamza.ecommerce.service;

import com.umhamza.ecommerce.dto.AddressRequest;
import com.umhamza.ecommerce.dto.OrderItemRequest;
import com.umhamza.ecommerce.dto.OrderRequest;
import com.umhamza.ecommerce.dto.OrderResponse;
import com.umhamza.ecommerce.entity.Address;
import com.umhamza.ecommerce.entity.Customer;
import com.umhamza.ecommerce.entity.Order;
import com.umhamza.ecommerce.entity.OrderItem;
import com.umhamza.ecommerce.entity.Product;
import com.umhamza.ecommerce.repository.AddressRepository;
import com.umhamza.ecommerce.repository.CustomerRepository;
import com.umhamza.ecommerce.repository.OrderItemRepository;
import com.umhamza.ecommerce.repository.OrderRepository;
import com.umhamza.ecommerce.repository.ProductRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private final CustomerRepository customerRepository;
    private final AddressRepository addressRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;

    public OrderService(
            CustomerRepository customerRepository,
            AddressRepository addressRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            ProductRepository productRepository) {
        this.customerRepository = customerRepository;
        this.addressRepository = addressRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public OrderResponse createOrder(
            Long customerId,
            OrderRequest request) {

        // 1. Validate customer
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Customer not found"));

        // 2. Create delivery address
        Address address = createAddress(
                customer,
                request.deliveryAddress());

        address = addressRepository.save(address);

        // 3. Create order
        Order order = new Order();

        order.setOrderNumber(generateOrderNumber());
        order.setCustomer(customer);
        order.setDeliveryAddress(address);
        order.setStatus("PENDING_VENDOR_APPROVAL");

        order.setDiscountAmount(BigDecimal.ZERO);
        order.setTaxAmount(BigDecimal.ZERO);
        order.setShippingCost(BigDecimal.ZERO);

        // 4. Validate products and reserve inventory
        BigDecimal subtotal = BigDecimal.ZERO;

        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderItemRequest itemRequest : request.items()) {

            Product product = productRepository
                    .findByProductIdForUpdate(
                            itemRequest.productId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Product not found: "
                                    + itemRequest.productId()));

            // Product must be active
            if (!product.isActive()) {
                throw new IllegalArgumentException(
                        "Product is currently unavailable: "
                                + product.getProductName());
            }

            // Calculate available inventory
            int availableQuantity = product.getStockQuantity()
                    - product.getReservedQuantity();

            // Validate inventory
            if (itemRequest.quantity() > availableQuantity) {
                throw new IllegalStateException(
                        "Insufficient stock for product: "
                                + product.getProductName());
            }

            // Determine the correct price
            BigDecimal unitPrice = product.getSalePrice() != null
                    ? product.getSalePrice()
                    : product.getPrice();

            if (unitPrice == null
                    || unitPrice.compareTo(BigDecimal.ZERO) < 0) {

                throw new IllegalStateException(
                        "Invalid price for product: "
                                + product.getProductName());
            }

            // Calculate item total
            BigDecimal lineTotal = unitPrice
                    .multiply(
                            BigDecimal.valueOf(
                                    itemRequest.quantity()))
                    .setScale(2, RoundingMode.HALF_UP);

            subtotal = subtotal.add(lineTotal);

            // Reserve inventory
            product.setReservedQuantity(
                    product.getReservedQuantity()
                            + itemRequest.quantity());

            // Create order item
            OrderItem orderItem = new OrderItem();

            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(itemRequest.quantity());
            orderItem.setUnitPrice(unitPrice);

            orderItems.add(orderItem);
        }

        // 5. Calculate order total
        subtotal = subtotal.setScale(
                2,
                RoundingMode.HALF_UP);

        BigDecimal totalAmount = subtotal;

        order.setSubtotal(subtotal);
        order.setTotalAmount(totalAmount);

        // 6. Save order
        Order savedOrder = orderRepository.save(order);

        // 7. Save order items
        orderItemRepository.saveAll(orderItems);

        // 8. Return response
        return new OrderResponse(
                savedOrder.getOrderId(),
                savedOrder.getOrderNumber(),
                savedOrder.getOrderDate(),
                savedOrder.getSubtotal(),
                savedOrder.getDiscountAmount(),
                savedOrder.getTaxAmount(),
                savedOrder.getShippingCost(),
                savedOrder.getTotalAmount(),
                savedOrder.getStatus());
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByCustomerId(Long customerId) {

        List<Order> orders = orderRepository
                .findByCustomer_CustomerId(customerId);

        return orders.stream()
                .map(order -> new OrderResponse(
                        order.getOrderId(),
                        order.getOrderNumber(),
                        order.getOrderDate(),
                        order.getSubtotal(),
                        order.getDiscountAmount(),
                        order.getTaxAmount(),
                        order.getShippingCost(),
                        order.getTotalAmount(),
                        order.getStatus()))
                .toList();
    }

    private Address createAddress(
            Customer customer,
            AddressRequest request) {

        Address address = new Address();

        address.setCustomer(customer);
        address.setAddressLabel(request.addressLabel());
        address.setAddressLine1(request.addressLine1());
        address.setAddressLine2(request.addressLine2());
        address.setCity(request.city());
        address.setEmirate(request.emirate());
        address.setPostalCode(request.postalCode());
        address.setCountry(request.country());
        address.setAddressType(request.addressType());
        address.setDefaultAddress(false);

        return address;
    }

    private String generateOrderNumber() {
        return "UH-" + UUID.randomUUID()
                .toString()
                .substring(0, 12)
                .toUpperCase();
    }
}