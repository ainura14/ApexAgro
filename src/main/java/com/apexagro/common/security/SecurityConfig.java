package com.apexagro.common.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * BE3-13 / BE3-08: безопасность инфраструктурных эндпоинтов.
 *
 * <ul>
 *   <li>Цепочка 1 (публичная): health, info, prometheus, OpenAPI и Swagger UI. На prod Swagger выключен
 *       настройками springdoc, а prometheus закрывается на уровне сети/Nginx (только для мониторинга).</li>
 *   <li>Цепочка «по умолчанию» (последняя): всё остальное требует аутентификации. Без неё API возвращает
 *       401 в формате RFC 7807.</li>
 * </ul>
 * Модуль identity (BE1) добавляет свою цепочку для {@code /api/**} с JWT и порядком между этими двумя
 * (например, {@code @Order(10)} + {@code securityMatcher("/api/**")}).
 */
@Configuration(proxyBeanMethods = false)
class SecurityConfig {

    static final String[] PUBLIC_INFRASTRUCTURE = {
            "/actuator/health", "/actuator/health/**", "/actuator/info", "/actuator/prometheus",
            "/v3/api-docs", "/v3/api-docs/**", "/v3/api-docs.yaml", "/swagger-ui.html", "/swagger-ui/**"
    };

    @Bean
    @Order(1)
    SecurityFilterChain infrastructureSecurity(HttpSecurity http) throws Exception {
        return http
                .securityMatcher(PUBLIC_INFRASTRUCTURE)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .build();
    }

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    SecurityFilterChain defaultDenySecurity(HttpSecurity http,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .exceptionHandling(ex -> ex
                        // 401/403 из фильтров — через тот же ApiExceptionHandler, что и ошибки контроллеров
                        .authenticationEntryPoint((request, response, e) -> resolver.resolveException(request, response, null, e))
                        .accessDeniedHandler((request, response, e) -> resolver.resolveException(request, response, null, e)))
                .build();
    }
}
