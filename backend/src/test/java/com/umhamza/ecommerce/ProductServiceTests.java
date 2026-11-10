
package com.umhamza.ecommerce;

import com.umhamza.ecommerce.dto.ProductRequest;
import com.umhamza.ecommerce.dto.ProductResponse;
import com.umhamza.ecommerce.entity.Product;
import com.umhamza.ecommerce.entity.Vendor;
import com.umhamza.ecommerce.repository.ProductRepository;
import com.umhamza.ecommerce.repository.VendorRepository;
import com.umhamza.ecommerce.service.ProductService;
import com.umhamza.ecommerce.service.ProductServiceImpl;

import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProductServiceTests {

    private ProductRepository productRepository;
    private VendorRepository vendorRepository;
    private ProductService productService;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        vendorRepository = mock(VendorRepository.class);

        productService = new ProductServiceImpl(
                productRepository,
                vendorRepository
        );
    }

    private ProductRequest validRequest(
            String sku,
            BigDecimal price,
            BigDecimal salePrice,
            Integer stockQuantity) {

        return new ProductRequest(
                1L,
                sku,
                "Test Product",
                "Unit test product",
                price,
                salePrice,
                new BigDecimal("250.00"),
                "g",
                stockQuantity,
                3,
                true
        );
    }

    private Vendor vendor() {
        Vendor vendor = new Vendor();
        vendor.setBusinessName("Test Vendor");
        return vendor;
    }

    @Test
    void createProductShouldSaveValidProduct() {
        Vendor vendor = vendor();

        when(productRepository.existsBySkuIgnoreCase("TEST-001"))
                .thenReturn(false);
        when(vendorRepository.findById(1L))
                .thenReturn(Optional.of(vendor));
        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> {
                    Product product = invocation.getArgument(0);
                    product.setProductId(10L);
                    return product;
                });

        ProductResponse response = productService.createProduct(
                validRequest(
                        "TEST-001",
                        new BigDecimal("10.00"),
                        new BigDecimal("9.00"),
                        12
                )
        );

        assertEquals(10L, response.productId());
        assertEquals("TEST-001", response.sku());
        assertEquals("Test Product", response.productName());
        assertEquals(12, response.stockQuantity());
        assertEquals("IN_STOCK", response.status());

        verify(productRepository).save(any(Product.class));
    }

    @Test
    void createProductShouldRejectDuplicateSku() {
        when(productRepository.existsBySkuIgnoreCase("TEST-001"))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> productService.createProduct(
                        validRequest(
                                "TEST-001",
                                new BigDecimal("10.00"),
                                null,
                                5
                        )
                )
        );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );

        verify(productRepository, never()).save(any(Product.class));
        verifyNoInteractions(vendorRepository);
    }

    @Test
    void createProductShouldRejectSalePriceAboveRegularPrice() {
        when(productRepository.existsBySkuIgnoreCase("TEST-002"))
                .thenReturn(false);
        when(vendorRepository.findById(1L))
                .thenReturn(Optional.of(vendor()));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> productService.createProduct(
                        validRequest(
                                "TEST-002",
                                new BigDecimal("10.00"),
                                new BigDecimal("12.00"),
                                5
                        )
                )
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void createProductShouldRejectMissingVendor() {
        when(productRepository.existsBySkuIgnoreCase("TEST-003"))
                .thenReturn(false);
        when(vendorRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> productService.createProduct(
                        validRequest(
                                "TEST-003",
                                new BigDecimal("10.00"),
                                null,
                                5
                        )
                )
        );

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void createProductWithZeroStockShouldBeOutOfStock() {
        Vendor vendor = vendor();

        when(productRepository.existsBySkuIgnoreCase("TEST-004"))
                .thenReturn(false);
        when(vendorRepository.findById(1L))
                .thenReturn(Optional.of(vendor));
        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> {
                    Product product = invocation.getArgument(0);
                    product.setProductId(11L);
                    return product;
                });

        ProductResponse response = productService.createProduct(
                validRequest(
                        "TEST-004",
                        new BigDecimal("10.00"),
                        null,
                        0
                )
        );

        assertEquals(0, response.stockQuantity());
        assertEquals("OUT_OF_STOCK", response.status());
    }
}
