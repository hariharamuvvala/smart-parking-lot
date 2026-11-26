package com.sahaj.parking.calendar;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Objects;

public final class WeekdayRules {
    private WeekdayRules() {
    }

    public static boolean isWeekdayExcludingHolidays(
            LocalDate date,
            PublicHolidayCalendar holidayCalendar) {
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(holidayCalendar, "holidayCalendar");

        DayOfWeek day = date.getDayOfWeek();
        return day != DayOfWeek.SATURDAY
                && day != DayOfWeek.SUNDAY
                && !holidayCalendar.isPublicHoliday(date);
    }
}
