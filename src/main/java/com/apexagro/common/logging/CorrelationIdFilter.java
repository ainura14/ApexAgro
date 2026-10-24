package com.apexagro.common.logging;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * BE3-05: берёт {@code X-Correlation-Id} из запроса (или генерирует), кладёт в MDC и возвращает в ответе.
 * Регистрируется раньше Spring Security, чтобы id был и в логах аутентификации.
 */
public class CorrelationIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String correlationId = CorrelationId.sanitizeOrGenerate(request.getHeader(CorrelationId.HEADER));
        String previous = MDC.get(CorrelationId.MDC_KEY);
        MDC.put(CorrelationId.MDC_KEY, correlationId);
        response.setHeader(CorrelationId.HEADER, correlationId);
        try {
            chain.doFilter(request, response);
        } finally {
            CorrelationId.restore(previous);
        }
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;   // SSE (ИИ-чат) и асинхронные ответы тоже с id
    }
}
