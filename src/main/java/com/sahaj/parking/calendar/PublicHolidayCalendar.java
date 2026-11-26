package com.sahaj.parking.calendar;

import java.time.LocalDate;

public interface PublicHolidayCalendar {
    boolean isPublicHoliday(LocalDate date);
}
