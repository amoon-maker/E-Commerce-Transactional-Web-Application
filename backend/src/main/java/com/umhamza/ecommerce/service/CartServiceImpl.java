package com.umhamza.ecommerce.service;

import com.umhamza.ecommerce.dto.CartItemResponse;
import com.umhamza.ecommerce.dto.CartResponse;
import com.umhamza.ecommerce.entity.CartItem;
import com.umhamza.ecommerce.entity.Customer;
import com.umhamza.ecommerce.entity.Product;
import com.umhamza.ecommerce.entity.ShoppingCart;
import com.umhamza.ecommerce.repository.CartItemRepository;
import com.umhamza.ecommerce.repository.ProductRepository;
import com.umhamza.ecommerce.repository.ShoppingCartRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CartServiceImpl implements CartService {

    private static final String ACTIVE = "ACTIVE";

    private final CustomerAccountService customerAccountService;
    private final ShoppingCartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    public CartServiceImpl(
            CustomerAccountService customerAccountService,
            ShoppingCartRepository cartRepository,
            CartItemRepository cartItemRepository,
            ProductRepository productRepository) {
        this.customerAccountService = customerAccountService;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(Authentication authentication) {
        Customer customer = customerAccountService.requireCustomer(authentication, false);
        return cartRepository.findByCustomerCustomerIdAndStatus(customer.getCustomerId(), ACTIVE)
                .map(this::toResponse)
                .orElseGet(() -> new CartResponse(List.of()));
    }

    @Transactional
    public CartResponse addItem(Authentication authentication, Long productId, Integer quantity) {
        Customer customer = customerAccountService.requireCustomer(authentication, true);
        Product product = requireProduct(productId);
        requireAvailable(product);
        requireQuantityAvailable(product, quantity);

        ShoppingCart cart = getOrCreateActiveCart(customer);
        CartItem item = cartItemRepository
                .findByCartCartIdAndProductProductId(cart.getCartId(), productId)
                .orElse(null);
        long targetQuantity = (item == null ? 0L : item.getQuantity()) + quantity;
        requireQuantityAvailable(product, targetQuantity);

        if (item == null) {
            cartItemRepository.save(new CartItem(cart, product, (int) targetQuantity));
        } else {
            item.setQuantity((int) targetQuantity);
            cartItemRepository.save(item);
        }
        return toResponse(cart);
    }

    @Transactional
    public CartResponse updateQuantity(
            Authentication authentication, Long productId, Integer quantity) {
        Customer customer = customerAccountService.requireCustomer(authentication, true);
        ShoppingCart cart = findActiveCart(customer);
        CartItem item = findItem(cart, productId);
        Product product = requireProduct(productId);
        requireAvailable(product);
        requireQuantityAvailable(product, quantity);

        item.setQuantity(quantity);
        cartItemRepository.save(item);
        return toResponse(cart);
    }

    @Transactional
    public CartResponse removeItem(Authentication authentication, Long productId) {
        Customer customer = customerAccountService.requireCustomer(authentication, true);
        cartRepository.findByCustomerCustomerIdAndStatus(customer.getCustomerId(), ACTIVE)
                .ifPresent(cart -> {
                    cartItemRepository.deleteByCartCartIdAndProductProductId(
                            cart.getCartId(), productId);
                });
        return getCart(authentication);
    }

    @Transactional
    public CartResponse clearCart(Authentication authentication) {
        Customer customer = customerAccountService.requireCustomer(authentication, true);
        cartRepository.findByCustomerCustomerIdAndStatus(customer.getCustomerId(), ACTIVE)
                .ifPresent(cart -> cartItemRepository.deleteByCartCartId(cart.getCartId()));
        return getCart(authentication);
    }

    private ShoppingCart getOrCreateActiveCart(Customer customer) {
        return cartRepository.findByCustomerCustomerIdAndStatus(customer.getCustomerId(), ACTIVE)
                .orElseGet(() -> cartRepository.save(new ShoppingCart(customer)));
    }

    private ShoppingCart findActiveCart(Customer customer) {
        return cartRepository.findByCustomerCustomerIdAndStatus(customer.getCustomerId(), ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("Cart item not found."));
    }

    private CartItem findItem(ShoppingCart cart, Long productId) {
        return cartItemRepository.findByCartCartIdAndProductProductId(
                        cart.getCartId(), productId)
                .orElseThrow(() -> new EntityNotFoundException("Cart item not found."));
    }

    private Product requireProduct(Long productId) {
        return productRepository.findByProductIdForUpdate(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found."));
    }

    private void requireAvailable(Product product) {
        if (!product.isActive() || availableStock(product) < 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Product is currently unavailable.");
        }
    }

    private void requireQuantityAvailable(Product product, long quantity) {
        if (quantity < 1 || quantity > availableStock(product)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Requested quantity exceeds available stock.");
        }
    }

    private CartResponse toResponse(ShoppingCart cart) {
        List<CartItemResponse> items = cartItemRepository
                .findByCartCartIdOrderByAddedAtAsc(cart.getCartId())
                .stream()
                .map(this::toResponse)
                .toList();
        return new CartResponse(items);
    }

    private CartItemResponse toResponse(CartItem item) {
        Product product = item.getProduct();
        BigDecimal price = product.getSalePrice() != null
                ? product.getSalePrice()
                : product.getPrice();
        return new CartItemResponse(
                item.getCartItemId(),
                product.getProductId(),
                product.getVendor().getVendorId(),
                product.getSku(),
                product.getProductName(),
                product.getDescription(),
                price,
                item.getQuantity(),
                availableStock(product),
                product.isActive());
    }

    private int availableStock(Product product) {
        return Math.max(0, product.getStockQuantity() - product.getReservedQuantity());
    }
}
