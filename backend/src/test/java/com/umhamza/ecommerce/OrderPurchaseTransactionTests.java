package com.umhamza.ecommerce;

import com.umhamza.ecommerce.dto.AddressRequest;
import com.umhamza.ecommerce.dto.OrderDetailResponse;
import com.umhamza.ecommerce.dto.OrderRequest;
import com.umhamza.ecommerce.dto.PaymentRequest;
import com.umhamza.ecommerce.entity.CartItem;
import com.umhamza.ecommerce.entity.Customer;
import com.umhamza.ecommerce.entity.Order;
import com.umhamza.ecommerce.entity.OrderItem;
import com.umhamza.ecommerce.entity.Product;
import com.umhamza.ecommerce.entity.Role;
import com.umhamza.ecommerce.entity.ShoppingCart;
import com.umhamza.ecommerce.entity.User;
import com.umhamza.ecommerce.entity.Vendor;
import com.umhamza.ecommerce.repository.AddressRepository;
import com.umhamza.ecommerce.repository.CartItemRepository;
import com.umhamza.ecommerce.repository.CustomerRepository;
import com.umhamza.ecommerce.repository.InvoiceRepository;
import com.umhamza.ecommerce.repository.OrderItemRepository;
import com.umhamza.ecommerce.repository.OrderRepository;
import com.umhamza.ecommerce.repository.PaymentRepository;
import com.umhamza.ecommerce.repository.ProductRepository;
import com.umhamza.ecommerce.repository.RoleRepository;
import com.umhamza.ecommerce.repository.ShoppingCartRepository;
import com.umhamza.ecommerce.repository.UserRepository;
import com.umhamza.ecommerce.repository.VendorRepository;
import com.umhamza.ecommerce.service.BankService;
import com.umhamza.ecommerce.service.OrderService;
import com.umhamza.ecommerce.service.PaymentServiceSelector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
                "spring.datasource.url=jdbc:h2:mem:purchase-transaction-test;MODE=MSSQLServer;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS dbo",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                "spring.security.user.password=integration-test-password"
        })
@Import(OrderPurchaseTransactionTests.TestBeans.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class OrderPurchaseTransactionTests {

    @Autowired private OrderService orderService;
    @Autowired private PaymentServiceSelector paymentServiceSelector;
    @Autowired private ControlledBankService controlledBankService;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private VendorRepository vendorRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ShoppingCartRepository cartRepository;
    @Autowired private CartItemRepository cartItemRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private AddressRepository addressRepository;
    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private PaymentRepository paymentRepository;

    @BeforeEach
    void configurePaymentService() {
        reset(paymentServiceSelector);
        controlledBankService.failNextPayment(false);
        when(paymentServiceSelector.getPaymentService("CREDIT"))
                .thenReturn(controlledBankService);
    }

    @Test
    void successfulPurchasePersistsOrderItemsReservationInvoiceAndPayment() {
        Fixture fixture = createFixture(10, 1, 2);

        OrderDetailResponse response = orderService.createOrder(
                fixture.authentication(), request("CREDIT"));

        Order order = orderRepository.findByOrderNumber(response.orderNumber()).orElseThrow();
        List<OrderItem> orderItems = orderItemRepository.findByOrder_OrderId(order.getOrderId());
        Product savedProduct = productRepository.findById(fixture.product().getProductId()).orElseThrow();

        assertEquals("PENDING_VENDOR_APPROVAL", order.getStatus());
        assertEquals(new BigDecimal("25.00"), order.getTotalAmount());
        assertEquals(1, orderItems.size());
        assertEquals(2, orderItems.getFirst().getQuantity());
        assertEquals(10, savedProduct.getStockQuantity());
        assertEquals(3, savedProduct.getReservedQuantity());
        assertEquals(7, savedProduct.getStockQuantity() - savedProduct.getReservedQuantity());
        assertTrue(cartItemRepository.findByCartCartIdOrderByAddedAtAsc(
                fixture.cart().getCartId()).isEmpty());
        assertEquals("CHECKED_OUT", cartRepository.findById(fixture.cart().getCartId())
                .orElseThrow().getStatus());

        var invoice = invoiceRepository.findByOrder_OrderId(order.getOrderId()).orElseThrow();
        var payment = paymentRepository
                .findFirstByOrder_OrderIdOrderByCreatedAtDesc(order.getOrderId()).orElseThrow();
        assertEquals("ISSUED", invoice.getStatus());
        assertEquals(new BigDecimal("25.00"), invoice.getTotalAmount());
        assertEquals("CREDIT", payment.getPaymentMethod());
        assertEquals("CAPTURED", payment.getStatus());
        assertEquals(new BigDecimal("25.00"), payment.getCapturedAmount());
        assertNotNull(response.invoice());
        assertNotNull(response.payment());
    }

    @Test
    void insufficientInventoryLeavesAllPurchaseRowsAndInventoryUnchanged() {
        Fixture fixture = createFixture(2, 1, 2);
        DatabaseCounts before = counts();

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> orderService.createOrder(fixture.authentication(), request("CREDIT")));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals(before, counts());
        Product savedProduct = productRepository.findById(fixture.product().getProductId()).orElseThrow();
        assertEquals(2, savedProduct.getStockQuantity());
        assertEquals(1, savedProduct.getReservedQuantity());
        assertFalse(cartItemRepository.findByCartCartIdOrderByAddedAtAsc(
                fixture.cart().getCartId()).isEmpty());
        assertEquals("ACTIVE", cartRepository.findById(fixture.cart().getCartId())
                .orElseThrow().getStatus());
    }

    @Test
    void paymentFailureRollsBackFlushedOrderItemsInventoryAndCartChanges() {
        Fixture fixture = createFixture(10, 1, 2);
        DatabaseCounts before = counts();
        controlledBankService.failNextPayment(true);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> orderService.createOrder(fixture.authentication(), request("CREDIT")));

        assertEquals(HttpStatus.PAYMENT_REQUIRED, error.getStatusCode());
        assertEquals(before, counts());
        Product savedProduct = productRepository.findById(fixture.product().getProductId()).orElseThrow();
        assertEquals(10, savedProduct.getStockQuantity());
        assertEquals(1, savedProduct.getReservedQuantity());
        assertEquals(0, orderRepository.findByCustomer_CustomerId(
                fixture.customer().getCustomerId()).size());
        assertFalse(cartItemRepository.findByCartCartIdOrderByAddedAtAsc(
                fixture.cart().getCartId()).isEmpty());
        assertEquals("ACTIVE", cartRepository.findById(fixture.cart().getCartId())
                .orElseThrow().getStatus());
    }

    private Fixture createFixture(int stock, int reserved, int requestedQuantity) {
        String suffix = UUID.randomUUID().toString();
        Role role = roleRepository.findByRoleName("CUSTOMER")
                .orElseGet(() -> roleRepository.save(new Role("CUSTOMER", "Customer")));
        User user = userRepository.save(new User("purchase-test-" + suffix + "@example.test",
                "not-used-by-test", role));
        Customer customer = customerRepository.save(new Customer(user, "Test", "Customer", null));
        Vendor vendor = vendorRepository.save(new Vendor("Purchase Test " + suffix,
                "United Arab Emirates"));

        Product product = new Product();
        product.setVendor(vendor);
        product.setSku("PURCHASE-" + suffix);
        product.setProductName("Purchase Test Product");
        product.setPrice(new BigDecimal("12.50"));
        product.setStockQuantity(stock);
        product.setReservedQuantity(reserved);
        product.setReorderLevel(0);
        product.setStatus("IN_STOCK");
        product.setActive(true);
        product = productRepository.save(product);

        ShoppingCart cart = cartRepository.save(new ShoppingCart(customer));
        cartItemRepository.save(new CartItem(cart, product, requestedQuantity));

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getEmail(), "ignored",
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        return new Fixture(customer, product, cart, authentication);
    }

    private OrderRequest request(String method) {
        AddressRequest address = new AddressRequest("Home", "Test Street", null,
                "Dubai", "Dubai", null, "United Arab Emirates", "HOME");
        return new OrderRequest(address, new PaymentRequest(method));
    }

    private DatabaseCounts counts() {
        return new DatabaseCounts(
                orderRepository.count(),
                orderItemRepository.count(),
                addressRepository.count(),
                invoiceRepository.count(),
                paymentRepository.count());
    }

    private record Fixture(
            Customer customer,
            Product product,
            ShoppingCart cart,
            Authentication authentication) {}

    private record DatabaseCounts(
            long orders,
            long orderItems,
            long addresses,
            long invoices,
            long payments) {}

    @TestConfiguration(proxyBeanMethods = false)
    static class TestBeans {
        @Bean
        @Primary
        PaymentServiceSelector transactionTestPaymentSelector() {
            return mock(PaymentServiceSelector.class);
        }

        @Bean
        ControlledBankService controlledBankService() {
            return new ControlledBankService();
        }
    }

    static class ControlledBankService implements BankService {
        private final AtomicBoolean failPayment = new AtomicBoolean();

        void failNextPayment(boolean fail) {
            failPayment.set(fail);
        }

        @Override
        public String processPayment(BigDecimal amount, String paymentReference) {
            if (failPayment.get()) {
                throw new IllegalStateException("Simulated payment failure.");
            }
            return "Simulated payment succeeded.";
        }
    }
}
