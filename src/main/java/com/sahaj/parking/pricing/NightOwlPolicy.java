package com.sahaj.parking.pricing;

import com.sahaj.parking.calendar.TimeWindow;
import com.sahaj.parking.domain.Money;
import com.sahaj.parking.domain.ParkingSession;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Objects;
import java.util.Optional;

public final class NightOwlPolicy implements RatePolicy {
    private static final LocalTime ENTRY_START = LocalTime.of(18, 0);
    private static final TimeWindow EXIT_WINDOW =
            new TimeWindow(LocalTime.of(5, 0), LocalTime.of(10, 0));
    private static final Money BASE_CAR_RATE = Money.ofCents(800);

    @Override
    public Optional<Money> calculate(ParkingSession session) {
        Objects.requireNonNull(session, "session");

        var ticket = session.ticket();
        if (!session.exitTimestamp().toLocalDate().equals(
                ticket.entryTimestamp().toLocalDate().plusDays(1))
                || ticket.entryTimestamp().toLocalTime().isBefore(ENTRY_START)
                || !EXIT_WINDOW.contains(session.exitTimestamp().toLocalTime())) {
            return Optional.empty();
        }

        BigDecimal discountFactor = BigDecimal.ONE.subtract(
                ticket.loyaltyTier().discountRate());
        BigDecimal fareFactor = discountFactor.multiply(
                ticket.vehicleType().rateMultiplier());
        return Optional.of(BASE_CAR_RATE.multipliedBy(fareFactor));
    }
}
