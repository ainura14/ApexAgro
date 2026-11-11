package com.apexagro.identity.internal.config;

import com.apexagro.identity.internal.security.JwtAuthConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
public class IdentitySecurityConfig {

    // Резервный публичный ключ для dev-режима (если файл не найден в ресурсах)
    private static final String DEFAULT_PUBLIC_KEY = """
            -----BEGIN PUBLIC KEY-----
            MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAi8V3uF+N6VZJYy450Yhc
            Rjk0+193YMw2+0xbccvHAp41tzN0ogVpDr/aHI0t9Dr5W7FLYXWEzG2pdkKR9+gU
            qL0125hUrR/AfLAtnhFHN+9oYasbUgkLSGUNRXeoDablGLYWmkwNmBpYCvewSGEX
            pHu03W6E5+2MzQ0b4+Ew/CXgaUmjP1Q+pgCbr0loCuFrrT4zTJM3ICtEFfSf88CT
            nYxpnAB2Y8TAsSa/RcVFtbLr4altHSyxAGwnyrSGVSmSC6woTFCPN9dKcMROwiEk
            6JuEWEWaUGnVnFWGd0i+1Wwf+VlOXYI5jh8wEqjwcL4qVB3VRHtFnncusJnRKRx7
            nQIDAQAB
            -----END PUBLIC KEY-----
            """;

    private final JwtAuthConverter jwtAuthConverter;
    private final HandlerExceptionResolver resolver;

    public IdentitySecurityConfig(
            JwtAuthConverter jwtAuthConverter,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver
    ) {
        this.jwtAuthConverter = jwtAuthConverter;
        this.resolver = resolver;
    }

    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.public-key-location:#{null}}") Resource publicKeyResource
    ) {
        RSAPublicKey publicKey = resolvePublicKey(publicKeyResource);
        return NimbusJwtDecoder.withPublicKey(publicKey).build();
    }

    private RSAPublicKey resolvePublicKey(Resource resource) {
        try {
            String pem;
            if (resource != null && resource.exists()) {
                try (InputStream is = resource.getInputStream()) {
                    pem = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
            } else {
                // Если файл не найден — используем резервный dev-ключ
                pem = DEFAULT_PUBLIC_KEY;
            }
            return parsePublicKey(pem);
        } catch (Exception e) {
            throw new IllegalStateException("Ошибка инициализации RSA публичного ключа", e);
        }
    }

    private RSAPublicKey parsePublicKey(String pem) throws Exception {
        String cleaned = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] encoded = Base64.getDecoder().decode(cleaned);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
        return (RSAPublicKey) keyFactory.generatePublic(keySpec);
    }

    @Bean
    @Order(10)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/api/**")
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 1. Публичные справочники
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/references/**",
                                "/api/v1/regions/**",
                                "/api/v1/districts/**",
                                "/api/v1/settlements/**",
                                "/api/v1/crops/**",
                                "/api/v1/categories/**").permitAll()

                        // 2. Публичные эндпоинты авторизации
                        .requestMatchers("/api/v1/auth/**").permitAll()

                        // 3. Админка
                        .requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN", "MODERATOR")

                        // 4. Все остальные запросы к API требуют JWT токен
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter))
                        .authenticationEntryPoint((request, response, e) ->
                                resolver.resolveException(request, response, null, e))
                        .accessDeniedHandler((request, response, e) ->
                                resolver.resolveException(request, response, null, e))
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, e) ->
                                resolver.resolveException(request, response, null, e))
                        .accessDeniedHandler((request, response, e) ->
                                resolver.resolveException(request, response, null, e))
                )
                .build();
    }
}