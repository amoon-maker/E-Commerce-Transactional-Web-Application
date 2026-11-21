package com.umhamza.ecommerce;

import com.umhamza.ecommerce.controller.AuthController;
import com.umhamza.ecommerce.entity.User;
import com.umhamza.ecommerce.entity.Vendor;
import com.umhamza.ecommerce.repository.UserRepository;
import com.umhamza.ecommerce.service.RegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthControllerTests {

    @Test
    void currentVendorIncludesItsVendorIdAndRole() {
        UserRepository userRepository = mock(UserRepository.class);
        AuthController controller = new AuthController(
                mock(RegistrationService.class),
                mock(AuthenticationManager.class),
                userRepository);
        Authentication authentication = mock(Authentication.class);
        User user = mock(User.class);
        Vendor vendor = mock(Vendor.class);

        when(authentication.getName()).thenReturn("vendor2@umhamza.local");
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_VENDOR")))
                .when(authentication).getAuthorities();
        when(userRepository.findByEmailIgnoreCase("vendor2@umhamza.local"))
                .thenReturn(Optional.of(user));
        when(user.getVendor()).thenReturn(vendor);
        when(vendor.getVendorId()).thenReturn(22L);

        ResponseEntity<?> response = controller.currentUser(authentication);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("vendor2@umhamza.local", body.get("email"));
        assertEquals(List.of("ROLE_VENDOR"), body.get("roles"));
        assertEquals(22L, body.get("vendorId"));
    }

    @Test
    void currentCustomerDoesNotReceiveVendorId() {
        UserRepository userRepository = mock(UserRepository.class);
        AuthController controller = new AuthController(
                mock(RegistrationService.class),
                mock(AuthenticationManager.class),
                userRepository);
        Authentication authentication = mock(Authentication.class);
        User user = mock(User.class);

        when(authentication.getName()).thenReturn("customer@umhamza.local");
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                .when(authentication).getAuthorities();
        when(userRepository.findByEmailIgnoreCase("customer@umhamza.local"))
                .thenReturn(Optional.of(user));
        when(user.getVendor()).thenReturn(null);

        ResponseEntity<?> response = controller.currentUser(authentication);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("customer@umhamza.local", body.get("email"));
        assertTrue(((List<?>) body.get("roles")).contains("ROLE_CUSTOMER"));
        assertFalse(body.containsKey("vendorId"));
    }
}
