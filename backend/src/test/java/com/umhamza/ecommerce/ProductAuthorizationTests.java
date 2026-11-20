package com.umhamza.ecommerce;

import com.umhamza.ecommerce.dto.ProductRequest;
import com.umhamza.ecommerce.dto.ProductResponse;
import com.umhamza.ecommerce.entity.Product;
import com.umhamza.ecommerce.entity.User;
import com.umhamza.ecommerce.entity.Vendor;
import com.umhamza.ecommerce.repository.ProductRepository;
import com.umhamza.ecommerce.repository.ProductVariantRepository;
import com.umhamza.ecommerce.repository.UserRepository;
import com.umhamza.ecommerce.repository.VendorRepository;
import com.umhamza.ecommerce.service.ProductService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProductAuthorizationTests {

    private ProductRepository productRepository;
    private VendorRepository vendorRepository;
    private ProductVariantRepository productVariantRepository;
    private UserRepository userRepository;
    private ProductService productService;

    private Vendor vendorOne;
    private Vendor vendorTwo;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        vendorRepository = mock(VendorRepository.class);
        productVariantRepository = mock(ProductVariantRepository.class);
        userRepository = mock(UserRepository.class);
        productService = new ProductService(
                productRepository,
                vendorRepository,
                productVariantRepository,
                userRepository
        );

        vendorOne = vendor(11L);
        vendorTwo = vendor(22L);

        User vendorUser = mock(User.class);
        when(vendorUser.getVendor()).thenReturn(vendorOne);
        when(userRepository.findByEmailIgnoreCase("vendor-one@example.com"))
                .thenReturn(Optional.of(vendorUser));

        when(vendorRepository.findById(11L)).thenReturn(Optional.of(vendorOne));
        when(vendorRepository.findById(22L)).thenReturn(Optional.of(vendorTwo));
        when(productVariantRepository.findByProduct_ProductIdAndActiveTrue(any()))
                .thenReturn(List.of());
    }

    @Test
    void vendorCanCreateUpdateAndDeleteOwnProducts() {
        when(productRepository.existsBySkuIgnoreCase("OWN-001"))
                .thenReturn(false);
        when(productRepository.existsBySkuIgnoreCaseAndProductIdNot("OWN-001", 101L))
                .thenReturn(false);
        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> {
                    Product product = invocation.getArgument(0);
                    product.setProductId(101L);
                    return product;
                });
        Product product = product(101L, vendorOne);
        when(productRepository.findById(101L)).thenReturn(Optional.of(product));

        ProductResponse created = productService.createProduct(
                request(11L, "OWN-001"), vendorAuthentication());
        ProductResponse updated = productService.updateProduct(
                101L, request(11L, "OWN-001"), vendorAuthentication());
        productService.deleteProduct(101L, vendorAuthentication());

        assertEquals(11L, created.vendorId());
        assertEquals(11L, updated.vendorId());
        verify(productRepository).delete(product);
    }

    @Test
    void vendorCannotCreateProductForAnotherVendor() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> productService.createProduct(
                        request(22L, "OTHER-001"), vendorAuthentication())
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verifyNoInteractions(productRepository, vendorRepository, productVariantRepository);
    }

    @Test
    void vendorCannotUpdateOrDeleteAnotherVendorsProduct() {
        Product otherVendorProduct = product(202L, vendorTwo);
        when(productRepository.findById(202L))
                .thenReturn(Optional.of(otherVendorProduct));

        ResponseStatusException updateException = assertThrows(
                ResponseStatusException.class,
                () -> productService.updateProduct(
                        202L, request(22L, "OTHER-002"), vendorAuthentication())
        );
        ResponseStatusException deleteException = assertThrows(
                ResponseStatusException.class,
                () -> productService.deleteProduct(202L, vendorAuthentication())
        );

        assertEquals(HttpStatus.FORBIDDEN, updateException.getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, deleteException.getStatusCode());
        verify(productRepository, never()).save(any(Product.class));
        verify(productRepository, never()).delete(any(Product.class));
    }

    @Test
    void vendorCannotTransferOwnProductToAnotherVendor() {
        Product ownProduct = product(303L, vendorOne);
        when(productRepository.findById(303L)).thenReturn(Optional.of(ownProduct));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> productService.updateProduct(
                        303L, request(22L, "OWN-303"), vendorAuthentication())
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(productRepository, never()).save(any(Product.class));
        verifyNoInteractions(vendorRepository);
    }

    @Test
    void adminCanCreateUpdateAndDeleteProductsAcrossVendors() {
        when(productRepository.existsBySkuIgnoreCase("ADMIN-001"))
                .thenReturn(false);
        when(productRepository.existsBySkuIgnoreCaseAndProductIdNot("ADMIN-002", 404L))
                .thenReturn(false);
        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> {
                    Product product = invocation.getArgument(0);
                    product.setProductId(404L);
                    return product;
                });
        Product otherVendorProduct = product(404L, vendorOne);
        when(productRepository.findById(404L))
                .thenReturn(Optional.of(otherVendorProduct));

        ProductResponse created = productService.createProduct(
                request(22L, "ADMIN-001"), adminAuthentication());
        ProductResponse updated = productService.updateProduct(
                404L, request(22L, "ADMIN-002"), adminAuthentication());
        productService.deleteProduct(404L, adminAuthentication());

        assertEquals(22L, created.vendorId());
        assertEquals(22L, updated.vendorId());
        verify(productRepository).delete(otherVendorProduct);
        verifyNoInteractions(userRepository);
    }

    private Vendor vendor(Long vendorId) {
        Vendor vendor = mock(Vendor.class);
        when(vendor.getVendorId()).thenReturn(vendorId);
        return vendor;
    }

    private Product product(Long productId, Vendor vendor) {
        Product product = new Product();
        product.setProductId(productId);
        product.setVendor(vendor);
        product.setSku("EXISTING-" + productId);
        product.setProductName("Existing product");
        product.setPrice(new BigDecimal("10.00"));
        product.setStockQuantity(10);
        product.setReservedQuantity(0);
        product.setReorderLevel(0);
        product.setStatus("IN_STOCK");
        product.setActive(true);
        return product;
    }

    private ProductRequest request(Long vendorId, String sku) {
        return new ProductRequest(
                vendorId,
                sku,
                "Authorization test product",
                "Test data",
                new BigDecimal("10.00"),
                null,
                null,
                null,
                10,
                0,
                true,
                List.of()
        );
    }

    private Authentication vendorAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                "vendor-one@example.com",
                "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_VENDOR"))
        );
    }

    private Authentication adminAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                "admin@example.com",
                "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }
}
