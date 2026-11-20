package com.umhamza.ecommerce;

import com.umhamza.ecommerce.dto.ProductResponse;
import com.umhamza.ecommerce.dto.VendorResponse;
import com.umhamza.ecommerce.controller.ProductController;
import com.umhamza.ecommerce.controller.VendorController;
import com.umhamza.ecommerce.service.ProductService;
import com.umhamza.ecommerce.service.VendorService;
import com.umhamza.ecommerce.config.SecurityConfig;

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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = {ProductController.class, VendorController.class},
        properties = "APP_DEV_PASSWORD=authorization-test-password")
@Import({SecurityConfig.class, ApiAuthorizationWebTests.TestBeans.class})
class ApiAuthorizationWebTests {

    private static final String PRODUCT_JSON = """
            {
              "vendorId": 11,
              "sku": "AUTH-TEST-001",
              "productName": "Authorization Test Product",
              "price": 10.00,
              "stockQuantity": 5,
              "reorderLevel": 0,
              "active": true,
              "variants": []
            }
            """;

    private static final String VENDOR_JSON = """
            {
              "businessName": "Authorization Test Vendor",
              "country": "United Arab Emirates"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductService productService;

    @Autowired
    private VendorService vendorService;

    @BeforeEach
    void resetMocks() {
        reset(productService, vendorService);
    }

    @Test
    void anonymousUsersCanReadCatalogButCannotMutateProductsOrManageVendors()
            throws Exception {

        when(productService.getAllProducts()).thenReturn(List.of());

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUCT_JSON))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/products/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUCT_JSON))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/products/1").with(csrf()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/vendors"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/vendors/1"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/vendors")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VENDOR_JSON))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/vendors/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VENDOR_JSON))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/vendors/1").with(csrf()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(vendorService);
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void customersCannotManageVendorsOrMutateProducts() throws Exception {

        assertVendorManagementDenied();

        mockMvc.perform(post("/api/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUCT_JSON))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/products/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUCT_JSON))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/products/1").with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(productService, vendorService);
    }

    @Test
    @WithMockUser(username = "vendor@example.com", roles = "VENDOR")
    void vendorsCannotUseVendorManagementEndpoints() throws Exception {

        assertVendorManagementDenied();
        verifyNoInteractions(vendorService);
    }

    @Test
    @WithMockUser(username = "vendor-one@example.com", roles = "VENDOR")
    void vendorCanReachProductMutationEndpointsForServiceOwnershipChecks()
            throws Exception {

        when(productService.createProduct(any(), any(Authentication.class)))
                .thenReturn(productResponse());
        when(productService.updateProduct(
                        eq(1L), any(), any(Authentication.class)))
                .thenReturn(productResponse());
        doNothing().when(productService)
                .deleteProduct(eq(1L), any(Authentication.class));

        mockMvc.perform(post("/api/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUCT_JSON))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/api/products/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUCT_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/products/1").with(csrf()))
                .andExpect(status().isNoContent());

        verify(productService).createProduct(any(), any(Authentication.class));
        verify(productService).updateProduct(
                eq(1L), any(), any(Authentication.class));
        verify(productService).deleteProduct(
                eq(1L), any(Authentication.class));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void adminsCanManageVendorsAndProducts() throws Exception {

        when(vendorService.getAllVendors()).thenReturn(List.of());
        when(vendorService.getVendorById(1L)).thenReturn(vendorResponse());
        when(vendorService.createVendor(any())).thenReturn(vendorResponse());
        when(vendorService.updateVendor(eq(1L), any()))
                .thenReturn(vendorResponse());
        doNothing().when(vendorService).deleteVendor(1L);

        when(productService.createProduct(any(), any(Authentication.class)))
                .thenReturn(productResponse());
        when(productService.updateProduct(
                        eq(1L), any(), any(Authentication.class)))
                .thenReturn(productResponse());
        doNothing().when(productService)
                .deleteProduct(eq(1L), any(Authentication.class));

        mockMvc.perform(get("/api/vendors"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/vendors/1"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/vendors")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VENDOR_JSON))
                .andExpect(status().isCreated());
        mockMvc.perform(put("/api/vendors/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VENDOR_JSON))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/vendors/1").with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUCT_JSON))
                .andExpect(status().isCreated());
        mockMvc.perform(put("/api/products/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUCT_JSON))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/products/1").with(csrf()))
                .andExpect(status().isNoContent());
    }

    private void assertVendorManagementDenied() throws Exception {
        mockMvc.perform(get("/api/vendors"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/vendors/1"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/vendors")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VENDOR_JSON))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/vendors/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VENDOR_JSON))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/vendors/1").with(csrf()))
                .andExpect(status().isForbidden());
    }

    private ProductResponse productResponse() {
        return new ProductResponse(
                1L,
                11L,
                "AUTH-TEST-001",
                "Authorization Test Product",
                null,
                new BigDecimal("10.00"),
                null,
                null,
                null,
                5,
                0,
                0,
                "IN_STOCK",
                true,
                0L,
                null,
                null,
                List.of()
        );
    }

    private VendorResponse vendorResponse() {
        return new VendorResponse(
                1L,
                "Authorization Test Vendor",
                null,
                null,
                null,
                null,
                null,
                null,
                "United Arab Emirates",
                true,
                null,
                null
        );
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestBeans {

        @Bean
        ProductService productService() {
            return mock(ProductService.class);
        }

        @Bean
        VendorService vendorService() {
            return mock(VendorService.class);
        }

        @Bean
        AuthenticationProvider authenticationProvider() {
            return new AuthenticationProvider() {
                @Override
                public Authentication authenticate(Authentication authentication) {
                    return null;
                }

                @Override
                public boolean supports(Class<?> authentication) {
                    return false;
                }
            };
        }
    }
}
