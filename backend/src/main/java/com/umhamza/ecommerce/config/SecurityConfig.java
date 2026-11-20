
package com.umhamza.ecommerce.config;

import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.Customizer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.function.Supplier;

@Configuration
public class SecurityConfig {

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthenticationProvider authenticationProvider) throws Exception {

        http
            .authenticationProvider(authenticationProvider)
            .cors(Customizer.withDefaults())

            .csrf(csrf -> csrf
                .csrfTokenRepository(
                    CookieCsrfTokenRepository.withHttpOnlyFalse()
                )
                .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
            )

            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/v3/api-docs",
                    "/v3/api-docs/**",
                    "/v3/api-docs.yaml",
                    "/api/auth/register",
                    "/api/auth/login",
                    "/api/auth/csrf"
                ).permitAll()

                .requestMatchers("/api/vendors", "/api/vendors/**")
                .hasRole("ADMIN")

                .requestMatchers("/api/cart", "/api/cart/**", "/api/favorites", "/api/favorites/**")
                .hasRole("CUSTOMER")

                .requestMatchers("/api/orders", "/api/orders/**")
                .hasRole("CUSTOMER")

                .requestMatchers("/api/vendor/orders", "/api/vendor/orders/**")
                .hasAnyRole("ADMIN", "VENDOR")

                .requestMatchers(
                    org.springframework.http.HttpMethod.GET,
                    "/api/products",
                    "/api/products/**"
                ).permitAll()

                .requestMatchers(
                    org.springframework.http.HttpMethod.POST,
                    "/api/products",
                    "/api/products/**"
                ).hasAnyRole("ADMIN", "VENDOR")

                .requestMatchers(
                    org.springframework.http.HttpMethod.PUT,
                    "/api/products",
                    "/api/products/**"
                ).hasAnyRole("ADMIN", "VENDOR")

                .requestMatchers(
                    org.springframework.http.HttpMethod.DELETE,
                    "/api/products",
                    "/api/products/**"
                ).hasAnyRole("ADMIN", "VENDOR")

                .anyRequest().authenticated()
            )

            .exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) ->
                    response.sendError(
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Unauthorized"
                    )
                )
            );

        return http.build();
    }

    private static final class SpaCsrfTokenRequestHandler
            implements CsrfTokenRequestHandler {

        private final CsrfTokenRequestAttributeHandler plain =
                new CsrfTokenRequestAttributeHandler();

        private final XorCsrfTokenRequestAttributeHandler xor =
                new XorCsrfTokenRequestAttributeHandler();

        @Override
        public void handle(
                HttpServletRequest request,
                HttpServletResponse response,
                Supplier<CsrfToken> csrfToken) {

            this.xor.handle(request, response, csrfToken);

            // Ensure the CSRF token is generated and stored in the cookie.
            csrfToken.get();
        }

        @Override
        public String resolveCsrfTokenValue(
                HttpServletRequest request,
                CsrfToken csrfToken) {

            String headerValue = request.getHeader(csrfToken.getHeaderName());

            // React sends the plain token in the request header.
            if (headerValue != null && !headerValue.isBlank()) {
                return this.plain.resolveCsrfTokenValue(request, csrfToken);
            }

            // Use the XOR handler for other requests.
            return this.xor.resolveCsrfTokenValue(request, csrfToken);
        }
    }
}
