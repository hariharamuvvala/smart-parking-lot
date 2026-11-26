package com.sahaj.parking.calendar;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

public record TimeWindow(LocalTime startInclusive, LocalTime endExclusive) {
    public TimeWindow {
        Objects.requireNonNull(startInclusive, "startInclusive");
        Objects.requireNonNull(endExclusive, "endExclusive");
        if (!startInclusive.isBefore(endExclusive)) {
            throw new IllegalArgumentException("Time window start must be before its end");
        }
    }

    public boolean contains(LocalTime time) {
        Objects.requireNonNull(time, "time");
        return !time.isBefore(startInclusive) && time.isBefore(endExclusive);
    }

    public boolean overlapsOnDate(
            LocalDate date,
            LocalDateTime intervalStart,
            LocalDateTime intervalEnd) {
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(intervalStart, "intervalStart");
        Objects.requireNonNull(intervalEnd, "intervalEnd");
        if (!intervalEnd.isAfter(intervalStart)) {
            throw new IllegalArgumentException("Interval end must be after its start");
        }

        LocalDateTime windowStart = date.atTime(startInclusive);
        LocalDateTime windowEnd = date.atTime(endExclusive);
        return intervalStart.isBefore(windowEnd) && intervalEnd.isAfter(windowStart);
    }
}
