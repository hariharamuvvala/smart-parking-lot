package com.sahaj.parking.calendar;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;

public final class FixedPublicHolidayCalendar implements PublicHolidayCalendar {
    private final Set<LocalDate> holidays;

    public FixedPublicHolidayCalendar(Set<LocalDate> holidays) {
        this.holidays = Set.copyOf(Objects.requireNonNull(holidays, "holidays"));
    }

    @Override
    public boolean isPublicHoliday(LocalDate date) {
        return holidays.contains(Objects.requireNonNull(date, "date"));
    }
}
