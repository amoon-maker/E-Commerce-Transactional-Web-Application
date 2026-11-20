package com.umhamza.ecommerce;

import com.umhamza.ecommerce.config.SecurityConfig;
import com.umhamza.ecommerce.controller.CartController;
import com.umhamza.ecommerce.controller.FavoriteController;
import com.umhamza.ecommerce.dto.CartResponse;
import com.umhamza.ecommerce.dto.FavoriteResponse;
import com.umhamza.ecommerce.service.CartService;
import com.umhamza.ecommerce.service.FavoriteService;
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
        controllers = {CartController.class, FavoriteController.class},
        properties = "APP_DEV_PASSWORD=cart-authorization-test-password")
@Import({SecurityConfig.class, CartFavoriteAuthorizationWebTests.TestBeans.class})
class CartFavoriteAuthorizationWebTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CartService cartService;

    @Autowired
    private FavoriteService favoriteService;

    @BeforeEach
    void resetMocks() {
        reset(cartService, favoriteService);
    }

    @Test
    void anonymousUsersCannotAccessCartOrFavorites() throws Exception {
        mockMvc.perform(get("/api/cart")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/favorites")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/cart/items").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":1,\"quantity\":1}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/favorites").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":1}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/cart").with(csrf()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/favorites/1").with(csrf()))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(cartService, favoriteService);
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void customersCanUseTheirCartAndFavoritesOperations() throws Exception {
        when(cartService.getCart(any(Authentication.class)))
                .thenReturn(new CartResponse(List.of()));
        when(cartService.addItem(any(Authentication.class), eq(1L), eq(2)))
                .thenReturn(new CartResponse(List.of()));
        when(cartService.updateQuantity(any(Authentication.class), eq(1L), eq(3)))
                .thenReturn(new CartResponse(List.of()));
        when(cartService.removeItem(any(Authentication.class), eq(1L)))
                .thenReturn(new CartResponse(List.of()));
        when(cartService.clearCart(any(Authentication.class)))
                .thenReturn(new CartResponse(List.of()));
        when(favoriteService.getFavorites(any(Authentication.class)))
                .thenReturn(List.of());
        when(favoriteService.addFavorite(any(Authentication.class), eq(1L)))
                .thenReturn(new FavoriteResponse(1L, "SKU-1", "Product", null, 4, true));
        doNothing().when(favoriteService).removeFavorite(any(Authentication.class), eq(1L));

        mockMvc.perform(get("/api/cart")).andExpect(status().isOk());
        mockMvc.perform(post("/api/cart/items").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":1,\"quantity\":2}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/cart/items/1").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":3}"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/cart/items/1").with(csrf()))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/cart").with(csrf()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/favorites")).andExpect(status().isOk());
        mockMvc.perform(post("/api/favorites").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":1}"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/favorites/1").with(csrf()))
                .andExpect(status().isNoContent());

        verify(cartService).getCart(any(Authentication.class));
        verify(cartService).addItem(any(Authentication.class), eq(1L), eq(2));
        verify(favoriteService).addFavorite(any(Authentication.class), eq(1L));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void cartRejectsNonPositiveQuantitiesBeforeCallingTheService() throws Exception {
        mockMvc.perform(post("/api/cart/items").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":1,\"quantity\":0}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(cartService);
    }

    @Test
    @WithMockUser(username = "vendor@example.com", roles = "VENDOR")
    void vendorsCannotUseCustomerCartOrFavoritesOperations() throws Exception {
        assertCustomerOperationsForbidden();
        verifyNoInteractions(cartService, favoriteService);
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void adminsCannotUseCustomerCartOrFavoritesOperations() throws Exception {
        assertCustomerOperationsForbidden();
        verifyNoInteractions(cartService, favoriteService);
    }

    private void assertCustomerOperationsForbidden() throws Exception {
        mockMvc.perform(get("/api/cart")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/favorites")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/cart/items").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":1,\"quantity\":1}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/favorites").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":1}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/cart").with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/favorites/1").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestBeans {
        @Bean
        CartService cartService() { return mock(CartService.class); }

        @Bean
        FavoriteService favoriteService() { return mock(FavoriteService.class); }

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
