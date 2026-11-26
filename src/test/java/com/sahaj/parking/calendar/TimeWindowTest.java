package com.sahaj.parking.calendar;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeWindowTest {
    private static final LocalDate DATE = LocalDate.of(2026, 11, 26);
    private static final TimeWindow MORNING_PEAK =
            new TimeWindow(LocalTime.of(7, 0), LocalTime.of(10, 0));

    @Test
    void treatsPeakStartAsInclusiveAndPeakEndAsExclusive() {
        assertTrue(MORNING_PEAK.overlapsOnDate(
                DATE,
                DATE.atTime(7, 0),
                DATE.atTime(8, 0)));
        assertFalse(MORNING_PEAK.overlapsOnDate(
                DATE,
                DATE.atTime(6, 0),
                DATE.atTime(7, 0)));
        assertFalse(MORNING_PEAK.overlapsOnDate(
                DATE,
                DATE.atTime(10, 0),
                DATE.atTime(11, 0)));
    }

    @Test
    void detectsAnyPositivePartialOverlap() {
        assertTrue(MORNING_PEAK.overlapsOnDate(
                DATE,
                DATE.atTime(6, 30),
                DATE.atTime(7, 30)));
        assertTrue(MORNING_PEAK.overlapsOnDate(
                DATE,
                DATE.atTime(9, 59),
                DATE.atTime(10, 59)));
    }

    @Test
    void evaluatesTheWindowOnTheRequestedDateForCrossMidnightIntervals() {
        LocalDate nextDate = DATE.plusDays(1);
        TimeWindow midnightWindow =
                new TimeWindow(LocalTime.MIDNIGHT, LocalTime.of(1, 0));

        assertTrue(midnightWindow.overlapsOnDate(
                nextDate,
                DATE.atTime(23, 30),
                nextDate.atTime(0, 30)));
        assertFalse(MORNING_PEAK.overlapsOnDate(
                nextDate,
                DATE.atTime(23, 30),
                nextDate.atTime(0, 30)));
    }

    @Test
    void rejectsInvalidWindowAndInterval() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new TimeWindow(LocalTime.NOON, LocalTime.NOON));
        assertThrows(
                IllegalArgumentException.class,
                () -> MORNING_PEAK.overlapsOnDate(
                        DATE,
                        LocalDateTime.of(DATE, LocalTime.NOON),
                        LocalDateTime.of(DATE, LocalTime.NOON)));
    }
}
