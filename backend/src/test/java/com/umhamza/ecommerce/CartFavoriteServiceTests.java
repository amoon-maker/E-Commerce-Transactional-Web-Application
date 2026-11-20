package com.umhamza.ecommerce;

import com.umhamza.ecommerce.dto.CartResponse;
import com.umhamza.ecommerce.dto.FavoriteResponse;
import com.umhamza.ecommerce.entity.CartItem;
import com.umhamza.ecommerce.entity.Customer;
import com.umhamza.ecommerce.entity.Favorite;
import com.umhamza.ecommerce.entity.Product;
import com.umhamza.ecommerce.entity.Role;
import com.umhamza.ecommerce.entity.ShoppingCart;
import com.umhamza.ecommerce.entity.User;
import com.umhamza.ecommerce.repository.CartItemRepository;
import com.umhamza.ecommerce.repository.CustomerRepository;
import com.umhamza.ecommerce.repository.FavoriteRepository;
import com.umhamza.ecommerce.repository.ProductRepository;
import com.umhamza.ecommerce.repository.ShoppingCartRepository;
import com.umhamza.ecommerce.repository.UserRepository;
import com.umhamza.ecommerce.service.CartService;
import com.umhamza.ecommerce.service.CartServiceImpl;
import com.umhamza.ecommerce.service.CustomerAccountService;
import com.umhamza.ecommerce.service.FavoriteService;
import com.umhamza.ecommerce.service.FavoriteServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CartFavoriteServiceTests {

    private Authentication authentication;
    private Customer customer;
    private CustomerAccountService customerAccountService;
    private ShoppingCartRepository cartRepository;
    private CartItemRepository cartItemRepository;
    private FavoriteRepository favoriteRepository;
    private ProductRepository productRepository;
    private CartServiceImpl cartService;
    private FavoriteServiceImpl favoriteService;
    private ShoppingCart cart;
    private Product product;

    @BeforeEach
    void setUp() {
        authentication = new UsernamePasswordAuthenticationToken(
                "customer@example.com", "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        customer = mock(Customer.class);
        when(customer.getCustomerId()).thenReturn(41L);
        customerAccountService = mock(CustomerAccountService.class);
        when(customerAccountService.requireCustomer(authentication, true))
                .thenReturn(customer);
        when(customerAccountService.requireCustomer(authentication, false))
                .thenReturn(customer);

        cartRepository = mock(ShoppingCartRepository.class);
        cartItemRepository = mock(CartItemRepository.class);
        favoriteRepository = mock(FavoriteRepository.class);
        productRepository = mock(ProductRepository.class);
        cartService = new CartServiceImpl(
                customerAccountService, cartRepository, cartItemRepository, productRepository);
        favoriteService = new FavoriteServiceImpl(
                customerAccountService, favoriteRepository, productRepository);

        cart = mock(ShoppingCart.class);
        when(cart.getCartId()).thenReturn(55L);
        product = product(901L, 10, 2, true);
    }

    @Test
    void accountIdentityIsResolvedFromTheAuthenticatedEmail() {
        UserRepository userRepository = mock(UserRepository.class);
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        User user = mock(User.class);
        Role role = mock(Role.class);
        Customer expectedCustomer = mock(Customer.class);
        Authentication alice = new UsernamePasswordAuthenticationToken(
                "alice@example.com", "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));

        when(userRepository.findByEmailIgnoreCase("alice@example.com"))
                .thenReturn(Optional.of(user));
        when(user.getUserId()).thenReturn(17L);
        when(user.isActive()).thenReturn(true);
        when(user.getRole()).thenReturn(role);
        when(role.getRoleName()).thenReturn("CUSTOMER");
        when(customerRepository.findByUserUserIdForUpdate(17L))
                .thenReturn(Optional.of(expectedCustomer));

        Customer resolved = new CustomerAccountService(userRepository, customerRepository)
                .requireCustomer(alice, true);

        assertSame(expectedCustomer, resolved);
        verify(userRepository).findByEmailIgnoreCase("alice@example.com");
        verify(customerRepository).findByUserUserIdForUpdate(17L);
    }

    @Test
    void nonCustomerRoleCannotResolveToCustomerOperations() {
        UserRepository userRepository = mock(UserRepository.class);
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        User vendor = mock(User.class);
        Role role = mock(Role.class);
        when(userRepository.findByEmailIgnoreCase("vendor@example.com"))
                .thenReturn(Optional.of(vendor));
        when(vendor.isActive()).thenReturn(true);
        when(vendor.getRole()).thenReturn(role);
        when(role.getRoleName()).thenReturn("VENDOR");
        Authentication vendorAuthentication = new UsernamePasswordAuthenticationToken(
                "vendor@example.com", "n/a");

        assertThrows(AccessDeniedException.class,
                () -> new CustomerAccountService(userRepository, customerRepository)
                        .requireCustomer(vendorAuthentication, false));
        verifyNoInteractions(customerRepository);
    }

    @Test
    void cartReadsAreScopedToTheAuthenticatedCustomersCart() {
        when(cartRepository.findByCustomerCustomerIdAndStatus(41L, "ACTIVE"))
                .thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartCartIdOrderByAddedAtAsc(55L))
                .thenReturn(List.of());

        CartResponse response = cartService.getCart(authentication);

        assertEquals(0, response.items().size());
        verify(customerAccountService).requireCustomer(authentication, false);
        verify(cartRepository).findByCustomerCustomerIdAndStatus(41L, "ACTIVE");
    }

    @Test
    void addingCartItemUsesAvailableStockAndDoesNotReserveInventory() {
        when(productRepository.findByProductIdForUpdate(901L))
                .thenReturn(Optional.of(product));
        when(cartRepository.findByCustomerCustomerIdAndStatus(41L, "ACTIVE"))
                .thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartCartIdAndProductProductId(55L, 901L))
                .thenReturn(Optional.empty());
        CartItem saved = mock(CartItem.class);
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> {
            CartItem item = invocation.getArgument(0);
            when(saved.getCartItemId()).thenReturn(88L);
            when(saved.getProduct()).thenReturn(product);
            when(saved.getQuantity()).thenReturn(item.getQuantity());
            return saved;
        });
        when(cartItemRepository.findByCartCartIdOrderByAddedAtAsc(55L))
                .thenReturn(List.of(saved));

        CartResponse response = cartService.addItem(authentication, 901L, 2);

        assertEquals(1, response.items().size());
        assertEquals(2, response.items().getFirst().quantity());
        assertEquals(8, response.items().getFirst().availableStock());
        verify(product, never()).setReservedQuantity(any());
    }

    @Test
    void addingExistingCartProductIncrementsItsQuantity() {
        when(productRepository.findByProductIdForUpdate(901L))
                .thenReturn(Optional.of(product));
        when(cartRepository.findByCustomerCustomerIdAndStatus(41L, "ACTIVE"))
                .thenReturn(Optional.of(cart));
        CartItem existing = mock(CartItem.class);
        when(existing.getQuantity()).thenReturn(2);
        when(existing.getProduct()).thenReturn(product);
        when(existing.getCartItemId()).thenReturn(88L);
        when(cartItemRepository.findByCartCartIdAndProductProductId(55L, 901L))
                .thenReturn(Optional.of(existing));
        when(cartItemRepository.findByCartCartIdOrderByAddedAtAsc(55L))
                .thenReturn(List.of(existing));

        cartService.addItem(authentication, 901L, 1);

        verify(existing).setQuantity(3);
        verify(cartItemRepository).save(existing);
    }

    @Test
    void addingMoreThanAvailableStockIsRejected() {
        when(productRepository.findByProductIdForUpdate(901L))
                .thenReturn(Optional.of(product));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> cartService.addItem(authentication, 901L, 9));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(cartItemRepository, never()).save(any(CartItem.class));
        verify(cartRepository, never()).save(any(ShoppingCart.class));
    }

    @Test
    void staleCartCannotBeUpdatedAboveCurrentAvailableStock() {
        CartItem existing = mock(CartItem.class);
        when(existing.getQuantity()).thenReturn(4);
        when(cartRepository.findByCustomerCustomerIdAndStatus(41L, "ACTIVE"))
                .thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartCartIdAndProductProductId(55L, 901L))
                .thenReturn(Optional.of(existing));
        Product stockChangedProduct = product(901L, 3, 1, true);
        when(productRepository.findByProductIdForUpdate(901L))
                .thenReturn(Optional.of(stockChangedProduct));
        when(cartItemRepository.findByCartCartIdOrderByAddedAtAsc(55L))
                .thenReturn(List.of());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> cartService.updateQuantity(authentication, 901L, 3));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(existing, never()).setQuantity(any());
        verify(cartItemRepository, never()).save(existing);
    }

    @Test
    void cartCannotAddAnUnknownProduct() {
        when(productRepository.findByProductIdForUpdate(901L))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> cartService.addItem(authentication, 901L, 1));
        verify(cartRepository, never()).save(any(ShoppingCart.class));
        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    void addingFavoriteIsIdempotentAndScopedToCustomer() {
        when(productRepository.findByProductIdForUpdate(901L))
                .thenReturn(Optional.of(product));
        Favorite existing = mock(Favorite.class);
        when(existing.getProduct()).thenReturn(product);
        when(favoriteRepository.findByCustomerCustomerIdAndProductProductId(41L, 901L))
                .thenReturn(Optional.of(existing));

        FavoriteResponse response = favoriteService.addFavorite(authentication, 901L);

        assertEquals(901L, response.productId());
        assertEquals(8, response.availableStock());
        verify(favoriteRepository)
                .findByCustomerCustomerIdAndProductProductId(41L, 901L);
        verify(favoriteRepository, never()).save(any(Favorite.class));
    }

    @Test
    void unavailableProductsCannotBeAddedToFavorites() {
        Product unavailableProduct = product(901L, 3, 3, true);
        when(productRepository.findByProductIdForUpdate(901L))
                .thenReturn(Optional.of(unavailableProduct));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> favoriteService.addFavorite(authentication, 901L));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(favoriteRepository, never()).save(any(Favorite.class));
    }

    @Test
    void favoritesCannotAddAnUnknownProduct() {
        when(productRepository.findByProductIdForUpdate(901L))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> favoriteService.addFavorite(authentication, 901L));
        verify(favoriteRepository, never()).save(any(Favorite.class));
    }

    @Test
    void removingFavoriteDeletesOnlyTheAuthenticatedCustomersRelation() {
        favoriteService.removeFavorite(authentication, 901L);

        verify(customerAccountService).requireCustomer(authentication, true);
        verify(favoriteRepository)
                .deleteByCustomerCustomerIdAndProductProductId(41L, 901L);
    }

    private Product product(Long productId, int stock, int reserved, boolean active) {
        Product product = mock(Product.class);
        when(product.getProductId()).thenReturn(productId);
        when(product.getSku()).thenReturn("TEST-" + productId);
        when(product.getProductName()).thenReturn("Test product");
        when(product.getDescription()).thenReturn("Test description");
        when(product.getPrice()).thenReturn(new BigDecimal("10.00"));
        when(product.getSalePrice()).thenReturn(null);
        when(product.getStockQuantity()).thenReturn(stock);
        when(product.getReservedQuantity()).thenReturn(reserved);
        when(product.isActive()).thenReturn(active);
        return product;
    }
}
