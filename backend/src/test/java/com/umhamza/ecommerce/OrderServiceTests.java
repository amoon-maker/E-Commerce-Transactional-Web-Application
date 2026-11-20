package com.umhamza.ecommerce;

import com.umhamza.ecommerce.dto.AddressRequest;
import com.umhamza.ecommerce.dto.OrderDetailResponse;
import com.umhamza.ecommerce.dto.OrderRequest;
import com.umhamza.ecommerce.dto.PaymentRequest;
import com.umhamza.ecommerce.entity.Address;
import com.umhamza.ecommerce.entity.CartItem;
import com.umhamza.ecommerce.entity.Customer;
import com.umhamza.ecommerce.entity.Delivery;
import com.umhamza.ecommerce.entity.Invoice;
import com.umhamza.ecommerce.entity.Order;
import com.umhamza.ecommerce.entity.OrderItem;
import com.umhamza.ecommerce.entity.Payment;
import com.umhamza.ecommerce.entity.Product;
import com.umhamza.ecommerce.entity.Role;
import com.umhamza.ecommerce.entity.ShoppingCart;
import com.umhamza.ecommerce.entity.User;
import com.umhamza.ecommerce.entity.Vendor;
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
import com.umhamza.ecommerce.service.BankService;
import com.umhamza.ecommerce.service.CustomerAccountService;
import com.umhamza.ecommerce.service.OrderService;
import com.umhamza.ecommerce.service.PaymentServiceSelector;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceTests {

    private CustomerAccountService customerAccountService;
    private UserRepository userRepository;
    private AddressRepository addressRepository;
    private ShoppingCartRepository cartRepository;
    private CartItemRepository cartItemRepository;
    private OrderRepository orderRepository;
    private OrderItemRepository orderItemRepository;
    private OrderStatusHistoryRepository statusHistoryRepository;
    private ProductRepository productRepository;
    private PaymentRepository paymentRepository;
    private InvoiceRepository invoiceRepository;
    private DeliveryRepository deliveryRepository;
    private RefundRepository refundRepository;
    private PaymentServiceSelector paymentServiceSelector;
    private OrderService orderService;

    private Authentication customerAuth;
    private Authentication vendorAuth;
    private Authentication adminAuth;
    private Customer customer;
    private User customerUser;
    private Vendor vendor;
    private Product product;
    private ShoppingCart cart;
    private CartItem cartItem;
    private OrderItem orderItem;

    @BeforeEach
    void setUp() {
        customerAccountService = mock(CustomerAccountService.class);
        userRepository = mock(UserRepository.class);
        addressRepository = mock(AddressRepository.class);
        cartRepository = mock(ShoppingCartRepository.class);
        cartItemRepository = mock(CartItemRepository.class);
        orderRepository = mock(OrderRepository.class);
        orderItemRepository = mock(OrderItemRepository.class);
        statusHistoryRepository = mock(OrderStatusHistoryRepository.class);
        productRepository = mock(ProductRepository.class);
        paymentRepository = mock(PaymentRepository.class);
        invoiceRepository = mock(InvoiceRepository.class);
        deliveryRepository = mock(DeliveryRepository.class);
        refundRepository = mock(RefundRepository.class);
        paymentServiceSelector = mock(PaymentServiceSelector.class);
        orderService = new OrderService(
                customerAccountService, userRepository, addressRepository,
                cartRepository, cartItemRepository, orderRepository,
                orderItemRepository, statusHistoryRepository, productRepository,
                paymentRepository, invoiceRepository, deliveryRepository,
                refundRepository, paymentServiceSelector);

        customerAuth = auth("customer@example.com", "CUSTOMER");
        vendorAuth = auth("vendor@example.com", "VENDOR");
        adminAuth = auth("admin@example.com", "ADMIN");
        customer = mock(Customer.class);
        customerUser = mock(User.class);
        when(customer.getCustomerId()).thenReturn(7L);
        when(customer.getUser()).thenReturn(customerUser);
        when(customerAccountService.requireCustomer(customerAuth, true)).thenReturn(customer);

        vendor = mock(Vendor.class);
        when(vendor.getVendorId()).thenReturn(42L);
        when(vendor.isActive()).thenReturn(true);
        product = product(vendor, 21L, 8, 1, "12.50");

        cart = mock(ShoppingCart.class);
        when(cart.getCartId()).thenReturn(81L);
        cartItem = new CartItem(cart, product, 2);
        orderItem = orderItem(product, 2, "12.50");

        when(addressRepository.save(any(Address.class))).thenAnswer(call -> call.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(call -> call.getArgument(0));
        when(orderItemRepository.saveAll(any())).thenAnswer(call -> call.getArgument(0));
        when(orderItemRepository.findByOrder_OrderId(anyLong())).thenReturn(List.of(orderItem));
        when(invoiceRepository.findByOrder_OrderId(anyLong())).thenReturn(Optional.empty());
        when(paymentRepository.findFirstByOrder_OrderIdOrderByCreatedAtDesc(anyLong()))
                .thenReturn(Optional.empty());
        when(deliveryRepository.findByOrder_OrderId(anyLong())).thenReturn(Optional.empty());
        when(statusHistoryRepository.findByOrder_OrderIdOrderByChangedAtAscHistoryIdAsc(anyLong()))
                .thenReturn(List.of());
    }

    @Test
    void checkoutReservesStockRecordsPendingStatusAndConsumesCartAtomically() {
        when(cartRepository.findByCustomerAndStatusForUpdate(7L, "ACTIVE"))
                .thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartCartIdOrderByAddedAtAsc(81L))
                .thenReturn(List.of(cartItem));
        when(productRepository.findByProductIdForUpdate(21L)).thenReturn(Optional.of(product));

        OrderDetailResponse response = orderService.createOrder(customerAuth, request());

        assertEquals("PENDING_VENDOR_APPROVAL", response.status());
        assertEquals(new BigDecimal("25.00"), response.totalAmount());
        assertEquals(3, product.getReservedQuantity());
        verify(orderItemRepository).saveAll(any());
        verify(statusHistoryRepository).save(any());
        verify(cartItemRepository).deleteByCartCartId(81L);
        verify(cartRepository).save(cart);
        verify(paymentRepository, never()).save(any());
        verify(invoiceRepository, never()).save(any(Invoice.class));
        verify(deliveryRepository, never()).save(any(Delivery.class));
    }

    @Test
    void checkoutRejectsUnavailableStockWithoutCreatingAnOrderOrClearingCart() {
        when(cartRepository.findByCustomerAndStatusForUpdate(7L, "ACTIVE"))
                .thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartCartIdOrderByAddedAtAsc(81L))
                .thenReturn(List.of(cartItem));
        product.setStockQuantity(2);
        product.setReservedQuantity(1);
        when(productRepository.findByProductIdForUpdate(21L)).thenReturn(Optional.of(product));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> orderService.createOrder(customerAuth, request()));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals(1, product.getReservedQuantity());
        verify(orderRepository, never()).save(any(Order.class));
        verify(cartItemRepository, never()).deleteByCartCartId(81L);
    }

    @Test
    void checkoutRejectsProductsFromDifferentVendors() {
        Vendor anotherVendor = mock(Vendor.class);
        when(anotherVendor.getVendorId()).thenReturn(77L);
        Product secondProduct = product(anotherVendor, 22L, 4, 0, "5.00");
        CartItem secondCartItem = new CartItem(cart, secondProduct, 1);
        when(cartRepository.findByCustomerAndStatusForUpdate(7L, "ACTIVE"))
                .thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartCartIdOrderByAddedAtAsc(81L))
                .thenReturn(List.of(cartItem, secondCartItem));
        when(productRepository.findByProductIdForUpdate(21L)).thenReturn(Optional.of(product));
        when(productRepository.findByProductIdForUpdate(22L)).thenReturn(Optional.of(secondProduct));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> orderService.createOrder(customerAuth, request()));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertEquals("Please place separate orders for products from different vendors.",
                error.getReason());
        verify(orderRepository, never()).save(any(Order.class));
        verify(cartItemRepository, never()).deleteByCartCartId(81L);
    }

    @Test
    void checkoutCannotBeSubmittedAgainAfterCartWasCheckedOut() {
        when(cartRepository.findByCustomerAndStatusForUpdate(7L, "ACTIVE"))
                .thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> orderService.createOrder(customerAuth, request()));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(orderRepository, never()).save(any(Order.class));
        verify(cartItemRepository, never()).deleteByCartCartId(anyLong());
    }

    @Test
    void vendorCanApproveOwnOrderAndPaymentWaitsForCustomerAction() {
        Order order = order("PENDING_VENDOR_APPROVAL", 55L);
        User actor = vendorUser(vendor);
        when(userRepository.findByEmailIgnoreCase("vendor@example.com"))
                .thenReturn(Optional.of(actor));
        when(orderRepository.findByOrderIdForUpdate(55L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrder_OrderId(55L)).thenReturn(List.of(orderItem));

        OrderDetailResponse response = orderService.approveOrder(55L, vendorAuth);

        assertEquals("APPROVED_AWAITING_PAYMENT", response.status());
        verify(statusHistoryRepository).save(any());
        verify(paymentRepository, never()).save(any());
        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    @Test
    void vendorCannotApproveAnotherVendorsOrder() {
        Vendor otherVendor = mock(Vendor.class);
        when(otherVendor.getVendorId()).thenReturn(88L);
        Order order = order("PENDING_VENDOR_APPROVAL", 56L);
        OrderItem unrelatedItem = orderItem(product(otherVendor, 24L, 5, 0, "2.00"), 1, "2.00");
        User actor = vendorUser(vendor);
        when(userRepository.findByEmailIgnoreCase("vendor@example.com"))
                .thenReturn(Optional.of(actor));
        when(orderRepository.findByOrderIdForUpdate(56L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrder_OrderId(56L)).thenReturn(List.of(unrelatedItem));

        assertThrows(AccessDeniedException.class,
                () -> orderService.approveOrder(56L, vendorAuth));

        assertEquals("PENDING_VENDOR_APPROVAL", order.getStatus());
        verify(statusHistoryRepository, never()).save(any());
    }

    @Test
    void adminCanApproveAnOrderForAnyVendor() {
        Vendor otherVendor = mock(Vendor.class);
        when(otherVendor.getVendorId()).thenReturn(88L);
        Order order = order("PENDING_VENDOR_APPROVAL", 63L);
        OrderItem anotherVendorItem = orderItem(product(otherVendor, 25L, 5, 0, "2.00"), 1, "2.00");
        User admin = mock(User.class);
        when(admin.isActive()).thenReturn(true);
        when(admin.getRole()).thenReturn(new Role("ADMIN", "Administrator"));
        when(userRepository.findByEmailIgnoreCase("admin@example.com"))
                .thenReturn(Optional.of(admin));
        when(orderRepository.findByOrderIdForUpdate(63L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrder_OrderId(63L)).thenReturn(List.of(anotherVendorItem));

        OrderDetailResponse response = orderService.approveOrder(63L, adminAuth);

        assertEquals("APPROVED_AWAITING_PAYMENT", response.status());
        verify(statusHistoryRepository).save(any());
    }

    @Test
    void rejectingOrderReleasesReservationAndCannotReleaseItTwice() {
        Order order = order("PENDING_VENDOR_APPROVAL", 57L);
        product.setReservedQuantity(2);
        User actor = vendorUser(vendor);
        when(userRepository.findByEmailIgnoreCase("vendor@example.com"))
                .thenReturn(Optional.of(actor));
        when(orderRepository.findByOrderIdForUpdate(57L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrder_OrderId(57L)).thenReturn(List.of(orderItem));
        when(productRepository.findByProductIdForUpdate(21L)).thenReturn(Optional.of(product));

        OrderDetailResponse rejected = orderService.rejectOrder(
                57L, "Item is temporarily unavailable", vendorAuth);
        assertEquals("REJECTED", rejected.status());
        assertEquals(0, product.getReservedQuantity());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> orderService.rejectOrder(57L, "Duplicate rejection", vendorAuth));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals(0, product.getReservedQuantity());
    }

    @Test
    void customerPaymentCaptureMovesApprovedOrderToProcessingAndCreatesFulfillmentRecords() {
        Order order = order("APPROVED_AWAITING_PAYMENT", 58L);
        order.setCustomer(customer);
        when(orderRepository.findByOrderIdForUpdate(58L)).thenReturn(Optional.of(order));
        BankService paymentService = mock(BankService.class);
        when(paymentServiceSelector.getPaymentService("CREDIT")).thenReturn(paymentService);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(call -> call.getArgument(0));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(call -> call.getArgument(0));

        OrderDetailResponse response = orderService.capturePayment(
                58L, customerAuth, new PaymentRequest("CREDIT"));

        assertEquals("PROCESSING", response.status());
        verify(paymentService).processPayment(eq(new BigDecimal("25.00")),
                org.mockito.ArgumentMatchers.matches("CREDIT-\\d{4}"));
        verify(paymentRepository).save(org.mockito.ArgumentMatchers.argThat(payment ->
                "CAPTURED".equals(payment.getStatus())
                        && payment.getCapturedAmount().compareTo(new BigDecimal("25.00")) == 0));
        verify(invoiceRepository).save(any(Invoice.class));
        verify(deliveryRepository).save(any(Delivery.class));

        ResponseStatusException retryError = assertThrows(ResponseStatusException.class,
                () -> orderService.capturePayment(58L, customerAuth, new PaymentRequest("CREDIT")));
        assertEquals(HttpStatus.CONFLICT, retryError.getStatusCode());
        verify(paymentService, times(1)).processPayment(any(), any());
        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(invoiceRepository, times(1)).save(any(Invoice.class));
        verify(deliveryRepository, times(1)).save(any(Delivery.class));
    }

    @Test
    void paymentCannotBeCapturedBeforeVendorApprovalOrForAnotherCustomer() {
        Order pending = order("PENDING_VENDOR_APPROVAL", 59L);
        pending.setCustomer(customer);
        when(orderRepository.findByOrderIdForUpdate(59L)).thenReturn(Optional.of(pending));

        ResponseStatusException statusError = assertThrows(ResponseStatusException.class,
                () -> orderService.capturePayment(59L, customerAuth, new PaymentRequest("DEBIT")));
        assertEquals(HttpStatus.CONFLICT, statusError.getStatusCode());

        Customer anotherCustomer = mock(Customer.class);
        when(anotherCustomer.getCustomerId()).thenReturn(8L);
        Order anotherCustomersOrder = order("APPROVED_AWAITING_PAYMENT", 60L);
        anotherCustomersOrder.setCustomer(anotherCustomer);
        when(orderRepository.findByOrderIdForUpdate(60L))
                .thenReturn(Optional.of(anotherCustomersOrder));
        assertThrows(EntityNotFoundException.class,
                () -> orderService.capturePayment(60L, customerAuth, new PaymentRequest("DEBIT")));
        verify(paymentRepository, never()).save(any());
        verify(paymentServiceSelector, never()).getPaymentService(any());
    }

    @Test
    void customerCancellationAfterCaptureReleasesStockRecordsRefundAndVoidsInvoice() {
        Order order = order("PROCESSING", 61L);
        order.setCustomer(customer);
        product.setReservedQuantity(2);
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentMethod("CREDIT");
        payment.setStatus("CAPTURED");
        payment.setAuthorizedAmount(new BigDecimal("25.00"));
        payment.setCapturedAmount(new BigDecimal("25.00"));
        Invoice invoice = new Invoice(order, "INV-TEST");
        Delivery delivery = new Delivery(order);
        when(orderRepository.findByOrderIdForUpdate(61L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrder_OrderId(61L)).thenReturn(List.of(orderItem));
        when(productRepository.findByProductIdForUpdate(21L)).thenReturn(Optional.of(product));
        when(paymentRepository.findFirstByOrder_OrderIdOrderByCreatedAtDesc(61L))
                .thenReturn(Optional.of(payment));
        when(invoiceRepository.findByOrder_OrderId(61L)).thenReturn(Optional.of(invoice));
        when(deliveryRepository.findByOrder_OrderId(61L))
        .thenReturn(Optional.of(delivery))
        .thenReturn(Optional.empty());

        OrderDetailResponse response = orderService.cancelOrder(61L, customerAuth);

        assertEquals("CANCELLED", response.status());
        assertEquals(0, product.getReservedQuantity());
        assertEquals("REFUNDED", payment.getStatus());
        assertEquals("VOID", invoice.getStatus());
        assertEquals(null, response.delivery());
        verify(refundRepository, times(1)).save(any());
        verify(paymentRepository, times(1)).save(payment);
        verify(deliveryRepository).delete(delivery);

        ResponseStatusException retryError = assertThrows(ResponseStatusException.class,
                () -> orderService.cancelOrder(61L, customerAuth));
        assertEquals(HttpStatus.CONFLICT, retryError.getStatusCode());
        assertEquals(0, product.getReservedQuantity());
        verify(refundRepository, times(1)).save(any());
        verify(paymentRepository, times(1)).save(payment);
        verify(deliveryRepository, times(1)).delete(delivery);
    }

    @Test
    void vendorDeliveryTransitionsOnlyConsumeInventoryWhenDelivered() {
        Order order = order("PROCESSING", 62L);
        product.setReservedQuantity(2);
        User actor = vendorUser(vendor);
        when(userRepository.findByEmailIgnoreCase("vendor@example.com"))
                .thenReturn(Optional.of(actor));
        when(orderRepository.findByOrderIdForUpdate(62L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrder_OrderId(62L)).thenReturn(List.of(orderItem));
        when(productRepository.findByProductIdForUpdate(21L)).thenReturn(Optional.of(product));
        Delivery delivery = new Delivery(order);
        when(deliveryRepository.findByOrder_OrderId(62L)).thenReturn(Optional.of(delivery));

        assertEquals("READY_FOR_DELIVERY", orderService.updateDeliveryStatus(
                62L, "READY_FOR_DELIVERY", vendorAuth).status());
        assertEquals(8, product.getStockQuantity());
        assertEquals(2, product.getReservedQuantity());
        assertEquals("OUT_FOR_DELIVERY", orderService.updateDeliveryStatus(
                62L, "OUT_FOR_DELIVERY", vendorAuth).status());
        assertEquals("DELIVERED", orderService.updateDeliveryStatus(
                62L, "DELIVERED", vendorAuth).status());
        assertEquals(6, product.getStockQuantity());
        assertEquals(0, product.getReservedQuantity());
        assertNotNull(delivery.getActualDeliveryDate());
        assertThrows(ResponseStatusException.class, () -> orderService.updateDeliveryStatus(
                62L, "DELIVERED", vendorAuth));
        assertEquals(6, product.getStockQuantity());
        assertEquals(0, product.getReservedQuantity());
    }

    @Test
    void customersCannotReadOrdersOwnedBySomeoneElse() {
        when(orderRepository.findByOrderIdAndCustomer_CustomerId(90L, 8L))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> orderService.getOrderDetailsByCustomerId(90L, 8L));
        verify(orderItemRepository, never()).findByOrder_OrderId(90L);
    }

    private Authentication auth(String email, String role) {
        return new UsernamePasswordAuthenticationToken(
                email, "ignored", List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }

    private OrderRequest request() {
        return new OrderRequest(new AddressRequest("Home", "Street 1", null,
                "Dubai", "Dubai", null, "United Arab Emirates", "HOME"));
    }

    private Product product(Vendor owner, long productId, int stock, int reserved, String price) {
        Product value = new Product();
        value.setProductId(productId);
        value.setVendor(owner);
        value.setSku("TEST-" + productId);
        value.setProductName("Test Product " + productId);
        value.setPrice(new BigDecimal(price));
        value.setStockQuantity(stock);
        value.setReservedQuantity(reserved);
        value.setActive(true);
        return value;
    }

    private OrderItem orderItem(Product forProduct, int quantity, String unitPrice) {
        OrderItem item = new OrderItem();
        item.setProduct(forProduct);
        item.setQuantity(quantity);
        item.setUnitPrice(new BigDecimal(unitPrice));
        return item;
    }

    private Order order(String status, Long orderId) {
        Address address = new Address();
        address.setAddressLabel("Home");
        address.setAddressLine1("Street 1");
        address.setCity("Dubai");
        address.setEmirate("Dubai");
        address.setCountry("United Arab Emirates");
        address.setAddressType("HOME");
        Order order = new Order();
        org.springframework.test.util.ReflectionTestUtils.setField(order, "orderId", orderId);
        order.setOrderNumber("UH-TEST-" + status);
        order.setCustomer(customer);
        order.setDeliveryAddress(address);
        order.setStatus(status);
        order.setSubtotal(new BigDecimal("25.00"));
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setTaxAmount(BigDecimal.ZERO);
        order.setShippingCost(BigDecimal.ZERO);
        order.setTotalAmount(new BigDecimal("25.00"));
        return order;
    }

    private User vendorUser(Vendor owner) {
        User user = mock(User.class);
        when(user.isActive()).thenReturn(true);
        when(user.getRole()).thenReturn(new Role("VENDOR", "Vendor"));
        when(user.getVendor()).thenReturn(owner);
        return user;
    }
}
