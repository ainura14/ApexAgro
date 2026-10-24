package com.apexagro.common.api;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * BE3-07: количество (кг, тонны, мешки и т. п.). В JSON — строка с ровно тремя знаками после точки: {@code "12.500"}.
 * Единица измерения передаётся отдельным полем ({@code unit}). На входе больше трёх знаков — ошибка 400.
 */
public record Quantity(BigDecimal value) implements Comparable<Quantity> {

    public static final int SCALE = 3;
    public static final Quantity ZERO = new Quantity(BigDecimal.ZERO);

    public Quantity {
        Objects.requireNonNull(value, "value");
        try {
            value = value.setScale(SCALE, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Количество должно иметь не больше " + SCALE + " знаков после точки: " + value, e);
        }
    }

    @JsonCreator
    public static Quantity parse(String value) {
        Objects.requireNonNull(value, "value");
        return new Quantity(new BigDecimal(value.trim()));
    }

    public static Quantity of(String value) {
        return parse(value);
    }

    public static Quantity of(BigDecimal value) {
        return new Quantity(value);
    }

    public Quantity plus(Quantity other) {
        return new Quantity(value.add(other.value));
    }

    public Quantity minus(Quantity other) {
        return new Quantity(value.subtract(other.value));
    }

    public boolean isPositive() {
        return value.signum() > 0;
    }

    @Override
    public int compareTo(Quantity other) {
        return value.compareTo(other.value);
    }

    @JsonValue
    @Override
    public String toString() {
        return value.toPlainString();
    }
}
