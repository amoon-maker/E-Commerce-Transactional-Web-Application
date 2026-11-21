package com.umhamza.ecommerce.service;

import com.umhamza.ecommerce.dto.AddressRequest;
import com.umhamza.ecommerce.dto.OrderDetailResponse;
import com.umhamza.ecommerce.dto.OrderRequest;
import com.umhamza.ecommerce.dto.OrderResponse;
import com.umhamza.ecommerce.dto.PaymentRequest;
import com.umhamza.ecommerce.entity.Address;
import com.umhamza.ecommerce.entity.CartItem;
import com.umhamza.ecommerce.entity.Customer;
import com.umhamza.ecommerce.entity.Delivery;
import com.umhamza.ecommerce.entity.Invoice;
import com.umhamza.ecommerce.entity.Order;
import com.umhamza.ecommerce.entity.OrderItem;
import com.umhamza.ecommerce.entity.OrderStatusHistory;
import com.umhamza.ecommerce.entity.Payment;
import com.umhamza.ecommerce.entity.Product;
import com.umhamza.ecommerce.entity.Refund;
import com.umhamza.ecommerce.entity.ShoppingCart;
import com.umhamza.ecommerce.entity.User;
import com.umhamza.ecommerce.repository.AddressRepository;
import com.umhamza.ecommerce.repository.CartItemRepository;
import com.umhamza.ecommerce.repository.DeliveryRepository;
import com.umhamza.ecommerce.repository.InvoiceRepository;
import com.umhamza.ecommerce.repository.OrderItemRepository;
import com.umhamza.ecommerce.repository.OrderRepository;
import com.umhamza.ecommerce.repository.OrderStatusHistoryRepository;
import com.umhamza.ecommerce.repository.PaymentRepository;
import com.umhamza.ecommerce.repository.ProductRepository;
import com.umhamza.ecommerce.repository.RefundRepository;
import com.umhamza.ecommerce.repository.ShoppingCartRepository;
import com.umhamza.ecommerce.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final String ACTIVE_CART = "ACTIVE";
    private static final String CHECKED_OUT_CART = "CHECKED_OUT";

    private final CustomerAccountService customerAccountService;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final ShoppingCartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository statusHistoryRepository;
    private final ProductRepository productRepository;
    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final DeliveryRepository deliveryRepository;
    private final RefundRepository refundRepository;
    private final PaymentServiceSelector paymentServiceSelector;

    public OrderService(
            CustomerAccountService customerAccountService,
            UserRepository userRepository,
            AddressRepository addressRepository,
            ShoppingCartRepository cartRepository,
            CartItemRepository cartItemRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            OrderStatusHistoryRepository statusHistoryRepository,
            ProductRepository productRepository,
            PaymentRepository paymentRepository,
            InvoiceRepository invoiceRepository,
            DeliveryRepository deliveryRepository,
            RefundRepository refundRepository,
            PaymentServiceSelector paymentServiceSelector) {
        this.customerAccountService = customerAccountService;
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.productRepository = productRepository;
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
        this.deliveryRepository = deliveryRepository;
        this.refundRepository = refundRepository;
        this.paymentServiceSelector = paymentServiceSelector;
    }

    @Transactional
    public OrderDetailResponse createOrder(
            Authentication authentication,
            OrderRequest request) {

        Customer customer = customerAccountService.requireCustomer(authentication, true);
        ShoppingCart cart = cartRepository
                .findByCustomerAndStatusForUpdate(customer.getCustomerId(), ACTIVE_CART)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Your active cart is empty or has already been checked out."));
        List<CartItem> cartItems = cartItemRepository
                .findByCartCartIdOrderByAddedAtAsc(cart.getCartId());
        if (cartItems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Your cart is empty.");
        }

        Address address = addressRepository.save(createAddress(
                customer, request.deliveryAddress()));
        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setCustomer(customer);
        order.setDeliveryAddress(address);
        order.setStatus("PENDING_VENDOR_APPROVAL");
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setTaxAmount(BigDecimal.ZERO);
        order.setShippingCost(BigDecimal.ZERO);

        BigDecimal subtotal = BigDecimal.ZERO;
        Long orderVendorId = null;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem cartItem : cartItems) {
            Product product = productRepository
                    .findByProductIdForUpdate(cartItem.getProduct().getProductId())
                    .orElseThrow(() -> new EntityNotFoundException("Product not found."));

            Long productVendorId = product.getVendor().getVendorId();
            if (orderVendorId == null) {
                orderVendorId = productVendorId;
            } else if (!orderVendorId.equals(productVendorId)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Please place separate orders for products from different vendors.");
            }

            if (!product.isActive()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Product is currently unavailable: " + product.getProductName());
            }
            int available = product.getStockQuantity() - product.getReservedQuantity();
            if (cartItem.getQuantity() < 1 || cartItem.getQuantity() > available) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Insufficient stock for product: " + product.getProductName());
            }

            BigDecimal unitPrice = product.getSalePrice() != null
                    ? product.getSalePrice()
                    : product.getPrice();
            if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Invalid price for product: " + product.getProductName());
            }

            subtotal = subtotal.add(unitPrice
                    .multiply(BigDecimal.valueOf(cartItem.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP));
            product.setReservedQuantity(product.getReservedQuantity() + cartItem.getQuantity());

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(unitPrice);
            orderItems.add(orderItem);
        }

        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
        if (subtotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Order total must be greater than zero.");
        }
        order.setSubtotal(subtotal);
        order.setTotalAmount(subtotal);

        Order savedOrder = orderRepository.save(order);
        orderItemRepository.saveAll(orderItems);
        statusHistoryRepository.save(new OrderStatusHistory(
                savedOrder,
                customer.getUser(),
                savedOrder.getStatus(),
                "Order submitted with simulated payment captured; awaiting vendor approval."));

        // Consume the saved cart in the same transaction as the order and reservations.
        cartItemRepository.deleteByCartCartId(cart.getCartId());
        cart.setStatus(CHECKED_OUT_CART);
        cartRepository.save(cart);

        // Payment is simulated locally. Keep it inside checkout's transaction so a
        // processing failure rolls back the order, items, reservation, address, and cart.
        // Flush staged rows first so integration tests exercise a real database rollback.
        orderRepository.flush();
        String method = request.payment().paymentMethod().trim().toUpperCase();
        recordCapturedPayment(savedOrder, method);
        ensureInvoice(savedOrder);

        return toOrderDetail(savedOrder);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByCustomerId(Long customerId) {
        List<Order> orders = orderRepository.findByCustomer_CustomerId(customerId);
        if (orders.isEmpty()) return List.of();

        Map<Long, String> invoiceNumbers = invoiceRepository
                .findByOrder_OrderIdIn(orders.stream().map(Order::getOrderId).toList())
                .stream()
                .collect(Collectors.toMap(
                        invoice -> invoice.getOrder().getOrderId(),
                        Invoice::getInvoiceNumber));

        return orders.stream().map(order -> new OrderResponse(
                order.getOrderId(),
                order.getOrderNumber(),
                order.getOrderDate(),
                order.getSubtotal(),
                order.getDiscountAmount(),
                order.getTaxAmount(),
                order.getShippingCost(),
                order.getTotalAmount(),
                order.getStatus(),
                invoiceNumbers.get(order.getOrderId()))).toList();
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderDetailsByCustomerId(Long orderId, Long customerId) {
        Order order = orderRepository
                .findByOrderIdAndCustomer_CustomerId(orderId, customerId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found."));
        return toOrderDetail(order);
    }

    @Transactional(readOnly = true)
    public List<OrderDetailResponse> getVendorOrders(Authentication authentication) {
        VendorActor actor = requireVendorActor(authentication);
        List<Order> orders = actor.admin()
                ? orderRepository.findAll(Sort.by(Sort.Direction.DESC, "orderDate"))
                : orderItemRepository.findOrdersByVendorId(actor.vendorId());

        return orders.stream()
                .filter(order -> actor.admin() || belongsToVendor(order, actor.vendorId()))
                .map(this::toOrderDetail)
                .toList();
    }

    @Transactional
    public OrderDetailResponse approveOrder(Long orderId, Authentication authentication) {
        VendorActor actor = requireVendorActor(authentication);
        Order order = requireVendorOrder(orderId, actor);
        if (!"PENDING_VENDOR_APPROVAL".equals(order.getStatus())) {
            throw conflict("Only pending orders can be approved.");
        }

        Payment existingPayment = latestPayment(orderId);
        if (existingPayment != null && "CAPTURED".equals(existingPayment.getStatus())) {
            // Support pending orders created by the earlier immediate-capture flow.
            order.setStatus("PROCESSING");
            ensureInvoice(order);
            ensureDelivery(order);
            saveStatus(order, actor.user(), "PROCESSING",
                    "Vendor approved an order whose simulated payment was already captured.");
        } else {
            order.setStatus("APPROVED_AWAITING_PAYMENT");
            orderRepository.save(order);
            saveStatus(order, actor.user(), order.getStatus(), "Vendor approved the order.");
        }
        return toOrderDetail(order);
    }

    @Transactional
    public OrderDetailResponse rejectOrder(
            Long orderId,
            String reason,
            Authentication authentication) {
        VendorActor actor = requireVendorActor(authentication);
        Order order = requireVendorOrder(orderId, actor);
        if (!"PENDING_VENDOR_APPROVAL".equals(order.getStatus())) {
            throw conflict("Only pending orders can be rejected.");
        }

        releaseReservations(orderId);
        refundCapturedPayment(order, "Vendor rejection: " + reason);
        order.setStatus("REJECTED");
        orderRepository.save(order);
        saveStatus(order, actor.user(), order.getStatus(), reason.trim());
        return toOrderDetail(order);
    }

    @Transactional
    public OrderDetailResponse capturePayment(
            Long orderId,
            Authentication authentication,
            PaymentRequest request) {
        Customer customer = customerAccountService.requireCustomer(authentication, true);
        Order order = orderRepository.findByOrderIdForUpdate(orderId)
                .filter(candidate -> candidate.getCustomer().getCustomerId()
                        .equals(customer.getCustomerId()))
                .orElseThrow(() -> new EntityNotFoundException("Order not found."));
        if (!"APPROVED_AWAITING_PAYMENT".equals(order.getStatus())) {
            throw conflict("Payment is available only after the vendor approves the order.");
        }

        String method = request.paymentMethod().trim().toUpperCase();
        recordCapturedPayment(order, method);

        order.setStatus("PROCESSING");
        orderRepository.save(order);
        ensureInvoice(order);
        ensureDelivery(order);
        saveStatus(order, order.getCustomer().getUser(), order.getStatus(),
                "Simulated payment captured; order is processing.");
        return toOrderDetail(order);
    }

    @Transactional
    public OrderDetailResponse cancelOrder(Long orderId, Authentication authentication) {
        Customer customer = customerAccountService.requireCustomer(authentication, true);
        Order order = orderRepository.findByOrderIdForUpdate(orderId)
                .filter(candidate -> candidate.getCustomer().getCustomerId()
                        .equals(customer.getCustomerId()))
                .orElseThrow(() -> new EntityNotFoundException("Order not found."));

        if (!List.of("PENDING_VENDOR_APPROVAL", "APPROVED_AWAITING_PAYMENT",
                "PROCESSING", "READY_FOR_DELIVERY").contains(order.getStatus())) {
            throw conflict("This order can no longer be cancelled.");
        }

        releaseReservations(orderId);
        refundCapturedPayment(order, "Customer cancelled before dispatch.");
        deliveryRepository.findByOrder_OrderId(orderId)
                .ifPresent(deliveryRepository::delete);
        order.setStatus("CANCELLED");
        orderRepository.save(order);
        saveStatus(order, order.getCustomer().getUser(), order.getStatus(),
                "Customer cancelled the order before dispatch.");
        return toOrderDetail(order);
    }

    @Transactional
    public OrderDetailResponse updateDeliveryStatus(
            Long orderId,
            String nextDeliveryStatus,
            Authentication authentication) {
        VendorActor actor = requireVendorActor(authentication);
        Order order = requireVendorOrder(orderId, actor);
        Delivery delivery = deliveryRepository.findByOrder_OrderId(orderId)
                .orElseThrow(() -> conflict("The order has no active delivery."));

        String nextOrderStatus;
        if ("PROCESSING".equals(order.getStatus())
                && "READY_FOR_DELIVERY".equals(nextDeliveryStatus)) {
            nextOrderStatus = "READY_FOR_DELIVERY";
        } else if ("READY_FOR_DELIVERY".equals(order.getStatus())
                && "OUT_FOR_DELIVERY".equals(nextDeliveryStatus)) {
            nextOrderStatus = "OUT_FOR_DELIVERY";
        } else if ("OUT_FOR_DELIVERY".equals(order.getStatus())
                && "DELIVERED".equals(nextDeliveryStatus)) {
            nextOrderStatus = "DELIVERED";
            completeInventorySale(orderId);
            delivery.setActualDeliveryDate(LocalDate.now());
        } else {
            throw conflict("Delivery status must move to the next allowed stage.");
        }

        delivery.setDeliveryStatus(nextDeliveryStatus);
        deliveryRepository.save(delivery);
        order.setStatus(nextOrderStatus);
        orderRepository.save(order);
        saveStatus(order, actor.user(), nextOrderStatus,
                "Vendor updated delivery status to " + nextDeliveryStatus + ".");
        return toOrderDetail(order);
    }

    private Order requireVendorOrder(Long orderId, VendorActor actor) {
        Order order = orderRepository.findByOrderIdForUpdate(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found."));
        if (!actor.admin() && !belongsToVendor(order, actor.vendorId())) {
            throw new AccessDeniedException("This order does not belong to your vendor account.");
        }
        return order;
    }

    private boolean belongsToVendor(Order order, Long vendorId) {
        List<OrderItem> items = orderItemRepository.findByOrder_OrderId(order.getOrderId());
        return !items.isEmpty() && items.stream().allMatch(item ->
                item.getProduct().getVendor().getVendorId().equals(vendorId));
    }

    private VendorActor requireVendorActor(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Vendor authentication is required.");
        }
        User user = userRepository.findByEmailIgnoreCase(authentication.getName())
                .filter(User::isActive)
                .orElseThrow(() -> new AccessDeniedException("Active account not found."));
        String roleName = user.getRole().getRoleName();
        if ("ADMIN".equals(roleName)) return new VendorActor(user, null, true);
        if (!"VENDOR".equals(roleName) || user.getVendor() == null
                || !user.getVendor().isActive()) {
            throw new AccessDeniedException("A vendor account is required.");
        }
        return new VendorActor(user, user.getVendor().getVendorId(), false);
    }

    private void releaseReservations(Long orderId) {
        for (OrderItem item : orderItemRepository.findByOrder_OrderId(orderId)) {
            Product product = productRepository.findByProductIdForUpdate(
                            item.getProduct().getProductId())
                    .orElseThrow(() -> new EntityNotFoundException("Product not found."));
            if (product.getReservedQuantity() < item.getQuantity()) {
                throw conflict("The inventory reservation is missing or was already released.");
            }
            product.setReservedQuantity(product.getReservedQuantity() - item.getQuantity());
            productRepository.save(product);
        }
    }

    private void completeInventorySale(Long orderId) {
        for (OrderItem item : orderItemRepository.findByOrder_OrderId(orderId)) {
            Product product = productRepository.findByProductIdForUpdate(
                            item.getProduct().getProductId())
                    .orElseThrow(() -> new EntityNotFoundException("Product not found."));
            if (product.getReservedQuantity() < item.getQuantity()
                    || product.getStockQuantity() < item.getQuantity()) {
                throw conflict("Inventory no longer matches this order reservation.");
            }
            product.setReservedQuantity(product.getReservedQuantity() - item.getQuantity());
            product.setStockQuantity(product.getStockQuantity() - item.getQuantity());
            productRepository.save(product);
        }
    }

    private void refundCapturedPayment(Order order, String reason) {
        Payment payment = latestPayment(order.getOrderId());
        if (payment == null) return;
        if ("AUTHORIZED".equals(payment.getStatus())) {
            payment.setStatus("CANCELLED");
            paymentRepository.save(payment);
            return;
        }
        if (!"CAPTURED".equals(payment.getStatus())) return;

        BigDecimal refundAmount = payment.getCapturedAmount();
        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw conflict("Captured payment amount is invalid; refund was not recorded.");
        }
        String refundReference = "SIM-REFUND-" + order.getOrderNumber();
        String storedReason = reason.length() > 300 ? reason.substring(0, 300) : reason;
        refundRepository.save(new Refund(payment, refundAmount, storedReason, refundReference));
        payment.setStatus("REFUNDED");
        paymentRepository.save(payment);
        invoiceRepository.findByOrder_OrderId(order.getOrderId()).ifPresent(invoice -> {
            invoice.setStatus("VOID");
            invoiceRepository.save(invoice);
        });
    }

    private Payment latestPayment(Long orderId) {
        return paymentRepository.findFirstByOrder_OrderIdOrderByCreatedAtDesc(orderId)
                .orElse(null);
    }

    private Payment recordCapturedPayment(Order order, String method) {
        BankService paymentService;
        try {
            paymentService = paymentServiceSelector.getPaymentService(method);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        }

        String reference = generatePaymentReference(method);
        try {
            paymentService.processPayment(order.getTotalAmount(), reference);
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(
                    HttpStatus.PAYMENT_REQUIRED,
                    "Payment could not be processed. No order changes were saved.",
                    exception);
        }

        Payment payment = latestPayment(order.getOrderId());
        if (payment == null || !"AUTHORIZED".equals(payment.getStatus())) {
            payment = new Payment();
            payment.setOrder(order);
        }
        payment.setPaymentMethod(method);
        payment.setStatus("CAPTURED");
        payment.setAuthorizedAmount(order.getTotalAmount());
        payment.setCapturedAmount(order.getTotalAmount());
        payment.setCurrencyCode("AED");
        payment.setProviderTransactionId(reference);
        return paymentRepository.save(payment);
    }

    private Invoice ensureInvoice(Order order) {
        return invoiceRepository.findByOrder_OrderId(order.getOrderId())
                .orElseGet(() -> invoiceRepository.save(new Invoice(
                        order, "INV-" + order.getOrderNumber())));
    }

    private Delivery ensureDelivery(Order order) {
        return deliveryRepository.findByOrder_OrderId(order.getOrderId())
                .orElseGet(() -> deliveryRepository.save(new Delivery(order)));
    }

    private void saveStatus(Order order, User user, String status, String comment) {
        statusHistoryRepository.save(new OrderStatusHistory(order, user, status, comment));
    }

    private OrderDetailResponse toOrderDetail(Order order) {
        List<OrderDetailResponse.ItemSummary> items = orderItemRepository
                .findByOrder_OrderId(order.getOrderId())
                .stream()
                .map(item -> new OrderDetailResponse.ItemSummary(
                        item.getProduct().getProductId(),
                        item.getProduct().getSku(),
                        item.getProduct().getProductName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                                .setScale(2, RoundingMode.HALF_UP)))
                .toList();

        Address address = order.getDeliveryAddress();
        OrderDetailResponse.AddressSummary addressSummary = new OrderDetailResponse.AddressSummary(
                address.getAddressLabel(), address.getAddressLine1(), address.getAddressLine2(),
                address.getCity(), address.getEmirate(), address.getPostalCode(),
                address.getCountry(), address.getAddressType());

        OrderDetailResponse.InvoiceSummary invoiceSummary = invoiceRepository
                .findByOrder_OrderId(order.getOrderId())
                .map(invoice -> new OrderDetailResponse.InvoiceSummary(
                        invoice.getInvoiceNumber(), invoice.getIssueDate(), invoice.getSubtotal(),
                        invoice.getDiscountAmount(), invoice.getTaxAmount(), invoice.getShippingCost(),
                        invoice.getTotalAmount(), invoice.getStatus()))
                .orElse(null);
        OrderDetailResponse.PaymentSummary paymentSummary = paymentRepository
                .findFirstByOrder_OrderIdOrderByCreatedAtDesc(order.getOrderId())
                .map(payment -> new OrderDetailResponse.PaymentSummary(
                        payment.getPaymentMethod(), payment.getStatus(), payment.getAuthorizedAmount(),
                        payment.getCapturedAmount(), payment.getCurrencyCode()))
                .orElse(null);
        OrderDetailResponse.DeliverySummary deliverySummary = deliveryRepository
                .findByOrder_OrderId(order.getOrderId())
                .map(delivery -> new OrderDetailResponse.DeliverySummary(
                        delivery.getDeliveryStatus(), delivery.getDeliveryMethod(),
                        delivery.getEstimatedDeliveryDate(), delivery.getActualDeliveryDate(),
                        delivery.getDeliveryNotes()))
                .orElse(null);
        List<OrderDetailResponse.StatusHistoryEntry> history = statusHistoryRepository
                .findByOrder_OrderIdOrderByChangedAtAscHistoryIdAsc(order.getOrderId())
                .stream()
                .map(entry -> new OrderDetailResponse.StatusHistoryEntry(
                        entry.getStatus(), entry.getComment(), entry.getChangedAt()))
                .toList();

        return new OrderDetailResponse(
                order.getOrderId(), order.getOrderNumber(), order.getOrderDate(), order.getStatus(),
                order.getSubtotal(), order.getDiscountAmount(), order.getTaxAmount(),
                order.getShippingCost(), order.getTotalAmount(), addressSummary, items,
                invoiceSummary, paymentSummary, deliverySummary, history);
    }

    private Address createAddress(Customer customer, AddressRequest request) {
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
        return "UH-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
    }

    private String generatePaymentReference(String method) {
        if ("CREDIT".equals(method)) {
            return String.format("CREDIT-%04d", ThreadLocalRandom.current().nextInt(10000));
        }
        if ("DEBIT".equals(method)) {
            return String.format("DEBIT-%06d", ThreadLocalRandom.current().nextInt(1000000));
        }
        throw new IllegalArgumentException("Unsupported payment method: " + method);
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    private record VendorActor(User user, Long vendorId, boolean admin) {}
}
