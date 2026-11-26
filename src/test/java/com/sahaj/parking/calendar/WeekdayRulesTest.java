package com.sahaj.parking.calendar;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeekdayRulesTest {
    private static final LocalDate THURSDAY = LocalDate.of(2026, 11, 26);

    @Test
    void treatsMondayThroughFridayAsWeekdaysExceptHolidays() {
        PublicHolidayCalendar holidays =
                new FixedPublicHolidayCalendar(Set.of(THURSDAY));

        assertTrue(WeekdayRules.isWeekdayExcludingHolidays(THURSDAY.minusDays(1), holidays));
        assertFalse(WeekdayRules.isWeekdayExcludingHolidays(THURSDAY, holidays));
        assertFalse(WeekdayRules.isWeekdayExcludingHolidays(THURSDAY.plusDays(2), holidays));
    }

    @Test
    void weekendIsNotAWeekdayEvenWhenNotAHoliday() {
        PublicHolidayCalendar holidays = new FixedPublicHolidayCalendar(Set.of());

        assertFalse(WeekdayRules.isWeekdayExcludingHolidays(THURSDAY.plusDays(2), holidays));
        assertFalse(WeekdayRules.isWeekdayExcludingHolidays(THURSDAY.plusDays(3), holidays));
    }
}
