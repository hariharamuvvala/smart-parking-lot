package com.sahaj.parking.pricing;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DurationRoundingTest {
    @Test
    void roundsPositivePartialHoursUp() {
        assertEquals(1, DurationRounding.ceilToWholeHours(Duration.ofNanos(1)));
        assertEquals(1, DurationRounding.ceilToWholeHours(Duration.ofMinutes(60)));
        assertEquals(2, DurationRounding.ceilToWholeHours(Duration.ofMinutes(61)));
    }

    @Test
    void preservesExactHoursAndRoundsFractionalNanosecondsUp() {
        assertEquals(2, DurationRounding.ceilToWholeHours(Duration.ofHours(2)));
        assertEquals(
                3,
                DurationRounding.ceilToWholeHours(Duration.ofHours(2).plusNanos(1)));
    }

    @Test
    void zeroDurationRoundsToZeroAndNegativeDurationIsRejected() {
        assertEquals(0, DurationRounding.ceilToWholeHours(Duration.ZERO));
        assertThrows(
                IllegalArgumentException.class,
                () -> DurationRounding.ceilToWholeHours(Duration.ofNanos(-1)));
    }
}
