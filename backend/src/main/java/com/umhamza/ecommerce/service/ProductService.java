package com.umhamza.ecommerce.service;

import com.umhamza.ecommerce.dto.ProductRequest;
import com.umhamza.ecommerce.dto.ProductResponse;
import com.umhamza.ecommerce.entity.Product;
import com.umhamza.ecommerce.entity.Vendor;
import com.umhamza.ecommerce.repository.ProductRepository;
import com.umhamza.ecommerce.repository.VendorRepository;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final VendorRepository vendorRepository;

    public ProductService(
            ProductRepository productRepository,
            VendorRepository vendorRepository) {

        this.productRepository = productRepository;
        this.vendorRepository = vendorRepository;
    }

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ProductResponse getProductById(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Product not found with ID: " + productId
                ));

        return toResponse(product);
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {

        String sku = request.sku().trim();

        if (productRepository.existsBySkuIgnoreCase(sku)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A product with this SKU already exists."
            );
        }

        Vendor vendor = vendorRepository.findById(request.vendorId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Vendor not found with ID: " + request.vendorId()
                ));

        if (request.salePrice() != null
                && request.salePrice().compareTo(request.price()) > 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Sale price cannot exceed the regular price."
            );
        }

        int stockQuantity = request.stockQuantity() == null
                ? 0
                : request.stockQuantity();

        int reorderLevel = request.reorderLevel() == null
                ? 0
                : request.reorderLevel();

        Product product = new Product();

        product.setVendor(vendor);
        product.setSku(sku);
        product.setProductName(request.productName().trim());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setSalePrice(request.salePrice());
        product.setPackageSize(request.packageSize());
        product.setPackageUnit(clean(request.packageUnit()));

        product.setStockQuantity(stockQuantity);
        product.setReservedQuantity(0);
        product.setReorderLevel(reorderLevel);

        // Keep the status consistent with the database stock rule.
        product.setStatus(
                stockQuantity == 0 ? "OUT_OF_STOCK" : "IN_STOCK"
        );

        product.setActive(
                request.active() == null || request.active()
        );

        Product savedProduct = productRepository.save(product);

        return toResponse(savedProduct);
    }

    @Transactional
public ProductResponse updateProduct(
        Long productId,
        ProductRequest request) {

    Product product = productRepository.findById(productId)
            .orElseThrow(() -> new EntityNotFoundException(
                    "Product not found with ID: " + productId
            ));

    String sku = request.sku().trim();

    if (productRepository
            .existsBySkuIgnoreCaseAndProductIdNot(sku, productId)) {

        throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "A product with this SKU already exists."
        );
    }

    Vendor vendor = vendorRepository.findById(request.vendorId())
            .orElseThrow(() -> new EntityNotFoundException(
                    "Vendor not found with ID: " + request.vendorId()
            ));

    if (request.salePrice() != null
            && request.salePrice().compareTo(request.price()) > 0) {

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Sale price cannot exceed the regular price."
        );
    }

    int newStockQuantity = request.stockQuantity() == null
            ? product.getStockQuantity()
            : request.stockQuantity();

    if (newStockQuantity < product.getReservedQuantity()) {
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Stock quantity cannot be lower than reserved quantity."
        );
    }

    product.setVendor(vendor);
    product.setSku(sku);
    product.setProductName(request.productName().trim());
    product.setDescription(request.description());
    product.setPrice(request.price());
    product.setSalePrice(request.salePrice());
    product.setPackageSize(request.packageSize());
    product.setPackageUnit(clean(request.packageUnit()));

    product.setStockQuantity(newStockQuantity);

    if (request.reorderLevel() != null) {
        product.setReorderLevel(request.reorderLevel());
    }

    if (request.active() != null) {
        product.setActive(request.active());
    }

    product.setStatus(
            newStockQuantity == 0
                    ? "OUT_OF_STOCK"
                    : "IN_STOCK"
    );

    Product updatedProduct = productRepository.save(product);

    return toResponse(updatedProduct);
}

@Transactional
public void deleteProduct(Long productId) {
    Product product = productRepository.findById(productId)
            .orElseThrow(() -> new EntityNotFoundException(
                    "Product not found with ID: " + productId
            ));

    productRepository.delete(product);
}

    private String clean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getProductId(),
                product.getVendor().getVendorId(),
                product.getSku(),
                product.getProductName(),
                product.getDescription(),
                product.getPrice(),
                product.getSalePrice(),
                product.getPackageSize(),
                product.getPackageUnit(),
                product.getStockQuantity(),
                product.getReservedQuantity(),
                product.getReorderLevel(),
                product.getStatus(),
                product.isActive(),
                product.getVersionNo(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}