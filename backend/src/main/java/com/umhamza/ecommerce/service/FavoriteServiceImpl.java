package com.umhamza.ecommerce.service;

import com.umhamza.ecommerce.dto.FavoriteResponse;
import com.umhamza.ecommerce.entity.Customer;
import com.umhamza.ecommerce.entity.Favorite;
import com.umhamza.ecommerce.entity.Product;
import com.umhamza.ecommerce.repository.FavoriteRepository;
import com.umhamza.ecommerce.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
public class FavoriteServiceImpl implements FavoriteService {

    private final CustomerAccountService customerAccountService;
    private final FavoriteRepository favoriteRepository;
    private final ProductRepository productRepository;

    public FavoriteServiceImpl(
            CustomerAccountService customerAccountService,
            FavoriteRepository favoriteRepository,
            ProductRepository productRepository) {
        this.customerAccountService = customerAccountService;
        this.favoriteRepository = favoriteRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<FavoriteResponse> getFavorites(Authentication authentication) {
        Customer customer = customerAccountService.requireCustomer(authentication, false);
        return favoriteRepository
                .findByCustomerCustomerIdOrderByCreatedAtDesc(customer.getCustomerId())
                .stream()
                .map(favorite -> toResponse(favorite.getProduct()))
                .toList();
    }

    @Transactional
    public FavoriteResponse addFavorite(Authentication authentication, Long productId) {
        Customer customer = customerAccountService.requireCustomer(authentication, true);
        Product product = productRepository.findByProductIdForUpdate(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found."));
        if (!product.isActive() || availableStock(product) < 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Product is currently unavailable.");
        }

        Favorite favorite = favoriteRepository
                .findByCustomerCustomerIdAndProductProductId(
                        customer.getCustomerId(), productId)
                .orElseGet(() -> favoriteRepository.save(new Favorite(customer, product)));
        return toResponse(favorite.getProduct());
    }

    @Transactional
    public void removeFavorite(Authentication authentication, Long productId) {
        Customer customer = customerAccountService.requireCustomer(authentication, true);
        favoriteRepository.deleteByCustomerCustomerIdAndProductProductId(
                customer.getCustomerId(), productId);
    }

    private FavoriteResponse toResponse(Product product) {
        BigDecimal price = product.getSalePrice() != null
                ? product.getSalePrice()
                : product.getPrice();
        return new FavoriteResponse(
                product.getProductId(),
                product.getSku(),
                product.getProductName(),
                price,
                availableStock(product),
                product.isActive());
    }

    private int availableStock(Product product) {
        return Math.max(0, product.getStockQuantity() - product.getReservedQuantity());
    }
}
