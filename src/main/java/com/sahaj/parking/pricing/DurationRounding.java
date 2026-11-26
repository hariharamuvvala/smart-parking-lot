package com.sahaj.parking.pricing;

import java.time.Duration;
import java.util.Objects;

public final class DurationRounding {
    private static final long SECONDS_PER_HOUR = 60 * 60;

    private DurationRounding() {
    }

    public static long ceilToWholeHours(Duration duration) {
        Objects.requireNonNull(duration, "duration");
        if (duration.isNegative()) {
            throw new IllegalArgumentException("Duration cannot be negative");
        }

        long fullHours = duration.getSeconds() / SECONDS_PER_HOUR;
        boolean hasRemainder = duration.getSeconds() % SECONDS_PER_HOUR != 0
                || duration.getNano() != 0;
        return hasRemainder ? Math.addExact(fullHours, 1) : fullHours;
    }
}
