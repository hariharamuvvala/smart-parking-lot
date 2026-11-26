package com.sahaj.parking.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MoneyTest {
    @Test
    void rejectsNegativeAmounts() {
        assertThrows(IllegalArgumentException.class, () -> Money.ofCents(-1));
    }

    @Test
    void addsCentsExactly() {
        assertEquals(Money.ofCents(800), Money.ofCents(500).plus(Money.ofCents(300)));
    }

    @Test
    void roundsMultiplicationToNearestCentUsingHalfUp() {
        assertEquals(
                Money.ofCents(81),
                Money.ofCents(101).multipliedBy(new BigDecimal("0.8")));
    }

    @Test
    void reportsOverflowWhenAddingCents() {
        assertThrows(
                ArithmeticException.class,
                () -> Money.ofCents(Long.MAX_VALUE).plus(Money.ofCents(1)));
    }
}
