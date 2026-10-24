package com.apexagro.common.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.apexagro.TestcontainersConfiguration;

import tools.jackson.databind.json.JsonMapper;

/** BE3-07 DoD: формат денег и количества — тест сериализации (через JsonMapper приложения). */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ApiConventionsJsonTest {

    @Autowired
    JsonMapper json;

    record Offer(Money price, Quantity quantity, Instant createdAt) {
    }

    @Test
    void moneyIsStringWithTwoDecimals() {
        assertThat(json.writeValueAsString(Money.of("1250"))).isEqualTo("\"1250.00\"");
        assertThat(json.writeValueAsString(Money.of(new BigDecimal("0.5")))).isEqualTo("\"0.50\"");
        assertThat(json.readValue("\"99.90\"", Money.class)).isEqualTo(Money.of("99.9"));
    }

    @Test
    void quantityIsStringWithThreeDecimals() {
        assertThat(json.writeValueAsString(Quantity.of("12.5"))).isEqualTo("\"12.500\"");
        assertThat(json.readValue("\"3\"", Quantity.class)).isEqualTo(Quantity.of("3.000"));
    }

    @Test
    void tooManyDecimalsIsRejectedNotRounded() {
        assertThatThrownBy(() -> Money.of("10.005")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> json.readValue("\"10.005\"", Money.class)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> Quantity.of("1.0001")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void datesAreIso8601Utc() {
        Offer offer = new Offer(Money.of("10"), Quantity.of("1"), Instant.parse("2026-10-24T10:15:30Z"));
        assertThat(json.writeValueAsString(offer))
                .isEqualTo("{\"price\":\"10.00\",\"quantity\":\"1.000\",\"createdAt\":\"2026-10-24T10:15:30Z\"}");
        assertThat(json.readValue(json.writeValueAsString(offer), Offer.class)).isEqualTo(offer);
    }

    @Test
    void pageFormat() {
        PageResponse<String> page = new PageResponse<>(List.of("a", "b"), 0, 20, 42);
        assertThat(json.writeValueAsString(page)).isEqualTo("{\"items\":[\"a\",\"b\"],\"page\":0,\"size\":20,\"total\":42}");
        assertThat(page.totalPages()).isEqualTo(3);
    }
}
