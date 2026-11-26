package com.sahaj.parking.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Money(long cents) {
    public Money {
        if (cents < 0) {
            throw new IllegalArgumentException("Money cannot be negative");
        }
    }

    public static Money ofCents(long cents) {
        return new Money(cents);
    }

    public Money plus(Money other) {
        Objects.requireNonNull(other, "other");
        return new Money(Math.addExact(cents, other.cents));
    }

    public Money multipliedBy(BigDecimal factor) {
        Objects.requireNonNull(factor, "factor");
        if (factor.signum() < 0) {
            throw new IllegalArgumentException("Money multiplier cannot be negative");
        }

        long result = BigDecimal.valueOf(cents)
                .multiply(factor)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
        return new Money(result);
    }
}
