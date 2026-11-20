
package com.umhamza.ecommerce.service;

import com.umhamza.ecommerce.dto.ProductRequest;
import com.umhamza.ecommerce.dto.ProductResponse;
import com.umhamza.ecommerce.dto.ProductVariantRequest;
import com.umhamza.ecommerce.dto.ProductVariantResponse;
import com.umhamza.ecommerce.entity.Product;
import com.umhamza.ecommerce.entity.ProductVariant;
import com.umhamza.ecommerce.entity.User;
import com.umhamza.ecommerce.entity.Vendor;
import com.umhamza.ecommerce.repository.ProductRepository;
import com.umhamza.ecommerce.repository.ProductVariantRepository;
import com.umhamza.ecommerce.repository.UserRepository;
import com.umhamza.ecommerce.repository.VendorRepository;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final VendorRepository vendorRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;

    public ProductService(
            ProductRepository productRepository,
            VendorRepository vendorRepository,
            ProductVariantRepository productVariantRepository,
            UserRepository userRepository) {

        this.productRepository = productRepository;
        this.vendorRepository = vendorRepository;
        this.productVariantRepository = productVariantRepository;
        this.userRepository = userRepository;
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
    public ProductResponse createProduct(
            ProductRequest request,
            Authentication authentication) {

        Long actorVendorId = getActorVendorId(authentication);
        requireVendorAccess(request.vendorId(), actorVendorId);

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

        validateProductSalePrice(
                request.price(),
                request.salePrice()
        );

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

        product.setStatus(
                stockQuantity == 0
                        ? "OUT_OF_STOCK"
                        : "IN_STOCK"
        );

        product.setActive(
                request.active() == null || request.active()
        );

        Product savedProduct = productRepository.save(product);

        // Create variants only when they are provided.
        if (request.variants() != null) {
            for (ProductVariantRequest variantRequest : request.variants()) {
                saveVariant(savedProduct, variantRequest);
            }
        }

        return toResponse(savedProduct);
    }



    @Transactional
    public ProductResponse updateProduct(
            Long productId,
            ProductRequest request,
            Authentication authentication) {

        Long actorVendorId = getActorVendorId(authentication);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Product not found with ID: " + productId
                ));

        requireVendorAccess(product.getVendor().getVendorId(), actorVendorId);
        requireVendorAccess(request.vendorId(), actorVendorId);

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

        validateProductSalePrice(
                request.price(),
                request.salePrice()
        );

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

        // null = leave existing variants unchanged.
        // [] = deactivate all existing variants.
        // Non-empty list = add or update the submitted variants.
        if (request.variants() != null) {

            if (request.variants().isEmpty()) {
                deactivateAllVariants(product);
            } else {
                for (ProductVariantRequest variantRequest : request.variants()) {
                    saveVariant(product, variantRequest);
                }
            }
        }

        return toResponse(updatedProduct);
    }



    private ProductVariant saveVariant(
            Product product,
            ProductVariantRequest request) {

        String sku = request.sku().trim();

        validateVariantSalePrice(
                request.price(),
                request.salePrice()
        );

        ProductVariant variant;

        if (request.variantId() == null) {

            // Creating a new variant.
            if (productVariantRepository.existsBySkuIgnoreCase(sku)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "A variant with this SKU already exists."
                );
            }

            variant = new ProductVariant();
            variant.setProduct(product);
            variant.setReservedQuantity(0);

        } else {

            // Updating an existing variant.
            variant = productVariantRepository.findById(request.variantId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Variant not found with ID: " + request.variantId()
                    ));

            if (!variant.getProduct().getProductId()
                    .equals(product.getProductId())) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "This variant does not belong to the selected product."
                );
            }

            if (productVariantRepository
                    .existsBySkuIgnoreCaseAndVariantIdNot(
                            sku,
                            request.variantId())) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "A variant with this SKU already exists."
                );
            }
        }

        int stockQuantity = request.stockQuantity() == null
                ? (variant.getStockQuantity() == null
                        ? 0
                        : variant.getStockQuantity())
                : request.stockQuantity();

        int reservedQuantity = variant.getReservedQuantity() == null
                ? 0
                : variant.getReservedQuantity();

        if (stockQuantity < reservedQuantity) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Variant stock cannot be lower than reserved quantity."
            );
        }

        int reorderLevel = request.reorderLevel() == null
                ? (variant.getReorderLevel() == null
                        ? 0
                        : variant.getReorderLevel())
                : request.reorderLevel();

        variant.setSku(sku);
        variant.setVariantName(request.variantName().trim());
        variant.setPackageSize(request.packageSize());
        variant.setPackageUnit(clean(request.packageUnit()));
        variant.setPrice(request.price());
        variant.setSalePrice(request.salePrice());

        variant.setStockQuantity(stockQuantity);
        variant.setReservedQuantity(reservedQuantity);
        variant.setReorderLevel(reorderLevel);

        variant.setStatus(
                stockQuantity == 0
                        ? "OUT_OF_STOCK"
                        : "IN_STOCK"
        );

        variant.setActive(
                request.active() == null || request.active()
        );

        return productVariantRepository.save(variant);
    }



    private void deactivateAllVariants(Product product) {

        List<ProductVariant> variants =
                productVariantRepository.findByProduct_ProductId(
                        product.getProductId()
                );

        for (ProductVariant variant : variants) {
            variant.setActive(false);
        }

        productVariantRepository.saveAll(variants);
    }



    @Transactional
    public void deleteProduct(
            Long productId,
            Authentication authentication) {

        Long actorVendorId = getActorVendorId(authentication);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Product not found with ID: " + productId
                ));

        requireVendorAccess(product.getVendor().getVendorId(), actorVendorId);

        productRepository.delete(product);
    }

    private Long getActorVendorId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw forbidden("Authentication is required to modify products.");
        }

        if (hasAuthority(authentication, "ROLE_ADMIN")) {
            return null;
        }

        if (!hasAuthority(authentication, "ROLE_VENDOR")) {
            throw forbidden("Only admins and vendors may modify products.");
        }

        User user = userRepository.findByEmailIgnoreCase(
                        authentication.getName())
                .orElseThrow(() -> forbidden(
                        "The authenticated vendor account was not found."));

        if (user.getVendor() == null || user.getVendor().getVendorId() == null) {
            throw forbidden("The vendor account is not linked to a vendor.");
        }

        return user.getVendor().getVendorId();
    }

    private boolean hasAuthority(
            Authentication authentication,
            String expectedAuthority) {

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(expectedAuthority::equals);
    }

    private void requireVendorAccess(
            Long resourceVendorId,
            Long actorVendorId) {

        // A null actor vendor ID represents the administrator bypass.
        if (actorVendorId != null && !actorVendorId.equals(resourceVendorId)) {
            throw forbidden("You may only modify products belonging to your vendor account.");
        }
    }

    private ResponseStatusException forbidden(String message) {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, message);
    }


    private void validateProductSalePrice(
            java.math.BigDecimal price,
            java.math.BigDecimal salePrice) {

        if (salePrice != null && salePrice.compareTo(price) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Sale price cannot exceed the regular price."
            );
        }
    }

    private void validateVariantSalePrice(
            java.math.BigDecimal price,
            java.math.BigDecimal salePrice) {

        if (salePrice != null && salePrice.compareTo(price) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Variant sale price cannot exceed its regular price."
            );
        }
    }

    private String clean(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }



    private ProductResponse toResponse(Product product) {

        List<ProductVariantResponse> variants =
                productVariantRepository
                        .findByProduct_ProductIdAndActiveTrue(
                                product.getProductId()
                        )
                        .stream()
                        .map(this::toVariantResponse)
                        .toList();

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
                product.getUpdatedAt(),
                variants
        );
    }

    private ProductVariantResponse toVariantResponse(
            ProductVariant variant) {

        return new ProductVariantResponse(
                variant.getVariantId(),
                variant.getSku(),
                variant.getVariantName(),
                variant.getPackageSize(),
                variant.getPackageUnit(),
                variant.getPrice(),
                variant.getSalePrice(),
                variant.getStockQuantity(),
                variant.getReservedQuantity(),
                variant.getReorderLevel(),
                variant.getStatus(),
                variant.isActive(),
                variant.getVersionNo(),
                variant.getCreatedAt(),
                variant.getUpdatedAt()
        );
    }
}
