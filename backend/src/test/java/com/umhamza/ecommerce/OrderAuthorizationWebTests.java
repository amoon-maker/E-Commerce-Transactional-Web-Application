package com.umhamza.ecommerce;

import com.umhamza.ecommerce.config.SecurityConfig;
import com.umhamza.ecommerce.controller.OrderController;
import com.umhamza.ecommerce.controller.VendorOrderController;
import com.umhamza.ecommerce.dto.OrderDetailResponse;
import com.umhamza.ecommerce.dto.OrderResponse;
import com.umhamza.ecommerce.entity.Customer;
import com.umhamza.ecommerce.service.CustomerAccountService;
import com.umhamza.ecommerce.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = {OrderController.class, VendorOrderController.class},
        properties = "APP_DEV_PASSWORD=order-authorization-test-password")
@Import({SecurityConfig.class, OrderAuthorizationWebTests.TestBeans.class})
class OrderAuthorizationWebTests {

    private static final String ORDER_JSON = """
            {
              "deliveryAddress": {
                "addressLabel": "Home",
                "addressLine1": "Street 1",
                "city": "Dubai",
                "emirate": "Dubai",
                "country": "United Arab Emirates",
                "addressType": "HOME"
              }
            }
            """;

    @Autowired private MockMvc mockMvc;
    @Autowired private OrderService orderService;
    @Autowired private CustomerAccountService customerAccountService;
    @Autowired private Customer customer;

    @BeforeEach
    void resetMocks() {
        reset(orderService, customerAccountService, customer);
        when(customer.getCustomerId()).thenReturn(7L);
    }

    @Test
    void anonymousUsersCannotUseCustomerOrVendorOrderEndpoints() throws Exception {
        mockMvc.perform(get("/api/orders/my-orders")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/orders").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(ORDER_JSON))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/vendor/orders")).andExpect(status().isUnauthorized());
        verifyNoInteractions(orderService, customerAccountService);
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void customerCanCreateReadPayAndCancelTheirOrders() throws Exception {
        when(customerAccountService.requireCustomer(any(Authentication.class), eq(false)))
                .thenReturn(customer);
        when(orderService.getOrdersByCustomerId(7L)).thenReturn(List.of(orderSummary()));
        when(orderService.createOrder(any(Authentication.class), any())).thenReturn(orderDetails());
        when(orderService.getOrderDetailsByCustomerId(55L, 7L)).thenReturn(orderDetails());
        when(orderService.capturePayment(eq(55L), any(Authentication.class), any()))
                .thenReturn(orderDetails());
        when(orderService.cancelOrder(eq(55L), any(Authentication.class)))
                .thenReturn(orderDetails());

        mockMvc.perform(get("/api/orders/my-orders")).andExpect(status().isOk());
        mockMvc.perform(get("/api/orders/my-orders/55")).andExpect(status().isOk());
        mockMvc.perform(post("/api/orders").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(ORDER_JSON))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/orders/my-orders/55/payment").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"paymentMethod\":\"CREDIT\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/orders/my-orders/55/cancel").with(csrf()))
                .andExpect(status().isOk());

        verify(orderService).createOrder(any(Authentication.class), any());
        verify(orderService).capturePayment(eq(55L), any(Authentication.class), any());
        verify(orderService).cancelOrder(eq(55L), any(Authentication.class));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void orderCreationRejectsAddressTypesOutsideTheDatabaseDomain() throws Exception {
        mockMvc.perform(post("/api/orders").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(ORDER_JSON.replace("\"HOME\"", "\"OFFICE\"")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(orderService, customerAccountService);
    }

    @Test
    @WithMockUser(username = "vendor@example.com", roles = "VENDOR")
    void vendorCanUseVendorOrderEndpointsButCannotUseCustomerOrderEndpoints() throws Exception {
        when(orderService.getVendorOrders(any(Authentication.class)))
                .thenReturn(List.of(orderDetails()));
        when(orderService.approveOrder(eq(55L), any(Authentication.class)))
                .thenReturn(orderDetails());
        when(orderService.rejectOrder(eq(55L), eq("Unavailable"), any(Authentication.class)))
                .thenReturn(orderDetails());
        when(orderService.updateDeliveryStatus(eq(55L), eq("READY_FOR_DELIVERY"),
                any(Authentication.class))).thenReturn(orderDetails());

        mockMvc.perform(get("/api/vendor/orders")).andExpect(status().isOk());
        mockMvc.perform(put("/api/vendor/orders/55/approve").with(csrf()))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/vendor/orders/55/reject").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Unavailable\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/vendor/orders/55/delivery-status").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"READY_FOR_DELIVERY\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/orders/my-orders")).andExpect(status().isForbidden());

        verify(orderService).getVendorOrders(any(Authentication.class));
        verifyNoInteractions(customerAccountService);
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void customersCannotUseVendorOrderEndpoints() throws Exception {
        mockMvc.perform(get("/api/vendor/orders")).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/vendor/orders/55/approve").with(csrf()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(orderService, customerAccountService);
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void adminsCanUseVendorOrderOperationsButCannotUseCustomerEndpoints() throws Exception {
        when(orderService.getVendorOrders(any(Authentication.class))).thenReturn(List.of());
        when(orderService.approveOrder(eq(55L), any(Authentication.class)))
                .thenReturn(orderDetails());

        mockMvc.perform(get("/api/vendor/orders")).andExpect(status().isOk());
        mockMvc.perform(put("/api/vendor/orders/55/approve").with(csrf()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/orders/my-orders")).andExpect(status().isForbidden());
        verifyNoInteractions(customerAccountService);
    }

    private OrderResponse orderSummary() {
        return new OrderResponse(55L, "UH-ORDER-55", LocalDateTime.now(),
                new BigDecimal("12.50"), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal("12.50"),
                "PENDING_VENDOR_APPROVAL", null);
    }

    private OrderDetailResponse orderDetails() {
        return new OrderDetailResponse(
                55L, "UH-ORDER-55", LocalDateTime.now(), "PENDING_VENDOR_APPROVAL",
                new BigDecimal("12.50"), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal("12.50"), null, List.of(), null,
                null, null, List.of());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestBeans {
        @Bean OrderService orderService() { return mock(OrderService.class); }
        @Bean CustomerAccountService customerAccountService() {
            return mock(CustomerAccountService.class);
        }
        @Bean Customer customer() { return mock(Customer.class); }
        @Bean AuthenticationProvider authenticationProvider() {
            return new AuthenticationProvider() {
                @Override public Authentication authenticate(Authentication authentication) {
                    return null;
                }
                @Override public boolean supports(Class<?> authentication) { return false; }
            };
        }
    }
}
