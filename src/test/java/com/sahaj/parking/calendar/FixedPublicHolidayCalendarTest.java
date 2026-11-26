package com.sahaj.parking.calendar;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FixedPublicHolidayCalendarTest {
    private static final LocalDate HOLIDAY = LocalDate.of(2026, 11, 26);

    @Test
    void reportsConfiguredDatesOnly() {
        FixedPublicHolidayCalendar calendar =
                new FixedPublicHolidayCalendar(Set.of(HOLIDAY));

        assertTrue(calendar.isPublicHoliday(HOLIDAY));
        assertFalse(calendar.isPublicHoliday(HOLIDAY.plusDays(1)));
    }

    @Test
    void copiesConfiguredDatesToPreventLaterMutation() {
        Set<LocalDate> dates = new HashSet<>(Set.of(HOLIDAY));
        FixedPublicHolidayCalendar calendar = new FixedPublicHolidayCalendar(dates);
        dates.clear();

        assertTrue(calendar.isPublicHoliday(HOLIDAY));
    }

    @Test
    void rejectsNullCalendarAndDate() {
        assertThrows(NullPointerException.class, () -> new FixedPublicHolidayCalendar(null));

        FixedPublicHolidayCalendar calendar =
                new FixedPublicHolidayCalendar(Set.of(HOLIDAY));
        assertThrows(NullPointerException.class, () -> calendar.isPublicHoliday(null));
    }
}
