package com.sahaj.parking.pricing;

import com.sahaj.parking.calendar.TimeWindow;
import com.sahaj.parking.domain.LoyaltyTier;
import com.sahaj.parking.domain.Money;
import com.sahaj.parking.domain.ParkingSession;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Objects;
import java.util.Optional;

public final class EarlyBirdPolicy implements RatePolicy {
    private static final TimeWindow ENTRY_WINDOW =
            new TimeWindow(LocalTime.of(6, 0), LocalTime.of(9, 0));
    private static final TimeWindow EXIT_WINDOW =
            new TimeWindow(LocalTime.of(15, 30), LocalTime.of(19, 0));
    private static final Money BASE_CAR_RATE = Money.ofCents(1500);

    @Override
    public Optional<Money> calculate(ParkingSession session) {
        Objects.requireNonNull(session, "session");

        var ticket = session.ticket();
        if (!ticket.entryTimestamp().toLocalDate().equals(session.exitTimestamp().toLocalDate())
                || !ENTRY_WINDOW.contains(ticket.entryTimestamp().toLocalTime())
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
