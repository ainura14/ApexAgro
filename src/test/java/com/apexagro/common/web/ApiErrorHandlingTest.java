package com.apexagro.common.web;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.apexagro.TestcontainersConfiguration;
import com.apexagro.common.api.Money;
import com.apexagro.common.error.ApiException;
import com.apexagro.common.error.CommonErrorCode;
import com.apexagro.common.ratelimit.RateLimitExceededException;

/**
 * BE3-04 / BE3-05 / BE3-06: любая ошибка — RFC 7807 с code и traceId, тексты на языке запроса,
 * X-Correlation-Id возвращается в ответе и попадает в traceId.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, ApiErrorHandlingTest.TestApi.class})
class ApiErrorHandlingTest {

    @Autowired
    MockMvc mvc;

    record CreateOffer(@NotBlank String name, @Size(max = 3) String note, Money price) {
    }

    @TestConfiguration
    @RestController
    static class TestApi {

        @PostMapping("/api/v1/test/offers")
        Money create(@Valid @RequestBody CreateOffer offer) {
            return offer.price();
        }

        @GetMapping("/api/v1/test/conflict")
        void conflict() {
            throw new ApiException(CommonErrorCode.CONFLICT).with("currentStatus", "DELIVERED");
        }

        @GetMapping("/api/v1/test/limited")
        void limited() {
            throw new RateLimitExceededException(java.time.Duration.ofSeconds(42));
        }

        @GetMapping("/api/v1/test/boom")
        void boom() {
            throw new IllegalStateException("секрет не должен утечь");
        }
    }

    @Test
    @WithMockUser
    void validationErrorsInRussian() throws Exception {
        mvc.perform(post("/api/v1/test/offers").header("Accept-Language", "ru")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\",\"note\":\"long\",\"price\":\"1.00\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.detail").value("Данные запроса заполнены неверно"))
                .andExpect(jsonPath("$.errors[?(@.field=='name')].code").value(hasItem("NotBlank")))
                .andExpect(jsonPath("$.errors[?(@.field=='name')].message").value(hasItem("Не должно быть пустым")))
                .andExpect(jsonPath("$.errors[?(@.field=='note')].message").value(hasItem("Длина должна быть от 0 до 3")))
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    @WithMockUser
    void validationErrorsInKyrgyz() throws Exception {
        mvc.perform(post("/api/v1/test/offers").header("Accept-Language", "ky")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\",\"price\":\"1.00\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Суроодогу маалыматтар туура эмес толтурулган"))
                .andExpect(jsonPath("$.errors[0].message").value("Бош болбошу керек"));
    }

    @Test
    @WithMockUser
    void malformedMoneyIsBadRequestNotServerError() throws Exception {
        mvc.perform(post("/api/v1/test/offers").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\",\"price\":\"1.005\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    @WithMockUser
    void apiExceptionWithExtraPropertiesAndCorrelationId() throws Exception {
        mvc.perform(get("/api/v1/test/conflict").header("X-Correlation-Id", "req-123"))
                .andExpect(status().isConflict())
                .andExpect(header().string("X-Correlation-Id", "req-123"))
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.currentStatus").value("DELIVERED"))
                .andExpect(jsonPath("$.traceId").value("req-123"));
    }

    @Test
    @WithMockUser
    void rateLimitHasRetryAfter() throws Exception {
        mvc.perform(get("/api/v1/test/limited"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "42"))
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"))
                .andExpect(jsonPath("$.retryAfterSeconds").value(42));
    }

    @Test
    @WithMockUser
    void unexpectedErrorDoesNotLeakDetails() throws Exception {
        mvc.perform(get("/api/v1/test/boom").header("Accept-Language", "ky"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.detail").value("Ички ката кетти. Кийинчерээк кайталап көрүңүз"));
    }

    @Test
    @WithMockUser
    void unknownPathIs404InOurFormat() throws Exception {
        mvc.perform(get("/api/v1/test/nope"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void unauthenticatedIs401InOurFormatWithGeneratedCorrelationId() throws Exception {
        mvc.perform(get("/api/v1/test/conflict"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(header().string("X-Correlation-Id", matchesPattern("[0-9a-f-]{36}")));
    }

    @Test
    void forgedCorrelationIdIsReplaced() throws Exception {
        mvc.perform(get("/actuator/health").header("X-Correlation-Id", "bad\nvalue"))
                .andExpect(header().string("X-Correlation-Id", matchesPattern("[0-9a-f-]{36}")));
    }
}
