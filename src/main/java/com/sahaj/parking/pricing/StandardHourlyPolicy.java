package com.sahaj.parking.pricing;

import com.sahaj.parking.calendar.PublicHolidayCalendar;
import com.sahaj.parking.calendar.TimeWindow;
import com.sahaj.parking.calendar.WeekdayRules;
import com.sahaj.parking.domain.Money;
import com.sahaj.parking.domain.ParkingSession;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class StandardHourlyPolicy implements RatePolicy {
    private static final BigDecimal PEAK_MULTIPLIER = new BigDecimal("1.5");
    private static final List<TimeWindow> PEAK_WINDOWS = List.of(
            new TimeWindow(LocalTime.of(7, 0), LocalTime.of(10, 0)),
            new TimeWindow(LocalTime.of(16, 0), LocalTime.of(19, 0)));

    private final PublicHolidayCalendar holidayCalendar;

    public StandardHourlyPolicy(PublicHolidayCalendar holidayCalendar) {
        this.holidayCalendar = Objects.requireNonNull(holidayCalendar, "holidayCalendar");
    }

    @Override
    public Optional<Money> calculate(ParkingSession session) {
        Objects.requireNonNull(session, "session");

        long billedHours = DurationRounding.ceilToWholeHours(session.duration());
        LocalDateTime blockStart = session.ticket().entryTimestamp();
        Money carTotal = Money.ofCents(0);

        for (long hour = 1; hour <= billedHours; hour++) {
            LocalDateTime blockEnd = blockStart.plusHours(1);
            Money blockRate = Money.ofCents(baseRateCents(hour));
            if (overlapsWeekdayPeak(blockStart, blockEnd)) {
                blockRate = blockRate.multipliedBy(PEAK_MULTIPLIER);
            }
            carTotal = carTotal.plus(blockRate);
            blockStart = blockEnd;
        }

        return Optional.of(carTotal.multipliedBy(
                session.ticket().vehicleType().rateMultiplier()));
    }

    private static long baseRateCents(long hourNumber) {
        if (hourNumber == 1) {
            return 500;
        }
        if (hourNumber == 2) {
            return 300;
        }
        return 200;
    }

    private boolean overlapsWeekdayPeak(LocalDateTime blockStart, LocalDateTime blockEnd) {
        LocalDate date = blockStart.toLocalDate();
        LocalDate lastDate = blockEnd.toLocalDate();

        while (!date.isAfter(lastDate)) {
            if (WeekdayRules.isWeekdayExcludingHolidays(date, holidayCalendar)) {
                for (TimeWindow window : PEAK_WINDOWS) {
                    if (window.overlapsOnDate(date, blockStart, blockEnd)) {
                        return true;
                    }
                }
            }
            if (date.equals(lastDate)) {
                break;
            }
            date = date.plusDays(1);
        }
        return false;
    }
}
