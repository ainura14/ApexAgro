package com.apexagro.common.api;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * BE3-07: денежная сумма в сомах (KGS). В JSON — строка с ровно двумя знаками после точки: {@code "1250.00"}.
 *
 * <p>Строка, а не число: JavaScript теряет точность на больших числах, а форма {@code "x.yy"} однозначна.
 * На входе больше двух знаков после точки — ошибка 400, без молчаливого округления.
 */
public record Money(BigDecimal amount) implements Comparable<Money> {

    public static final int SCALE = 2;
    public static final Money ZERO = new Money(BigDecimal.ZERO);

    public Money {
        Objects.requireNonNull(amount, "amount");
        try {
            amount = amount.setScale(SCALE, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Сумма должна иметь не больше " + SCALE + " знаков после точки: " + amount, e);
        }
    }

    @JsonCreator
    public static Money parse(String value) {
        Objects.requireNonNull(value, "value");
        return new Money(new BigDecimal(value.trim()));
    }

    public static Money of(String value) {
        return parse(value);
    }

    public static Money of(BigDecimal value) {
        return new Money(value);
    }

    public Money plus(Money other) {
        return new Money(amount.add(other.amount));
    }

    public Money minus(Money other) {
        return new Money(amount.subtract(other.amount));
    }

    /** Умножение на количество (например, цена за кг × кг). Округление — банковское, до 2 знаков. */
    public Money times(BigDecimal factor) {
        return new Money(amount.multiply(factor).setScale(SCALE, RoundingMode.HALF_EVEN));
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    @Override
    public int compareTo(Money other) {
        return amount.compareTo(other.amount);
    }

    @JsonValue
    @Override
    public String toString() {
        return amount.toPlainString();
    }
}
