
package com.umhamza.ecommerce.config;

import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.Customizer;

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
            jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED,
            "Unauthorized"
        )
    )
);

        return http.build();
    }
}
