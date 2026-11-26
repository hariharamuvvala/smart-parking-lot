package com.sahaj.parking.pricing;

import com.sahaj.parking.calendar.FixedPublicHolidayCalendar;
import com.sahaj.parking.domain.Fare;
import com.sahaj.parking.domain.LoyaltyTier;
import com.sahaj.parking.domain.Money;
import com.sahaj.parking.domain.ParkingSession;
import com.sahaj.parking.domain.ParkingTicket;
import com.sahaj.parking.domain.PolicyName;
import com.sahaj.parking.domain.VehicleType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParkingFeeCalculatorTest {
    private static final LocalDate DATE = LocalDate.of(2026, 11, 26);

    @Test
    void selectsEarlyBirdWhenItIsTheLowestValidFare() {
        ParkingFeeCalculator calculator = calculator(Set.of());
        ParkingSession session = session(
                LoyaltyTier.GOLD,
                DATE.atTime(6, 30),
                DATE.atTime(16, 0));

        assertEquals(
                new Fare(Money.ofCents(1200), PolicyName.EARLY_BIRD),
                calculator.calculate(session));
    }

    @Test
    void selectsNightOwlWhenItIsTheLowestValidFare() {
        ParkingFeeCalculator calculator = calculator(Set.of());
        ParkingSession session = session(
                LoyaltyTier.SILVER,
                DATE.atTime(18, 0),
                DATE.plusDays(1).atTime(6, 0));

        assertEquals(
                new Fare(Money.ofCents(720), PolicyName.NIGHT_OWL),
                calculator.calculate(session));
    }

    @Test
    void selectsStandardWhenNoSpecialPolicyApplies() {
        ParkingFeeCalculator calculator = calculator(Set.of());
        ParkingSession session = session(
                LoyaltyTier.NONE,
                LocalDate.of(2026, 11, 28).atTime(10, 0),
                LocalDate.of(2026, 11, 28).atTime(11, 0));

        assertEquals(
                new Fare(Money.ofCents(500), PolicyName.STANDARD_HOURLY),
                calculator.calculate(session));
    }

    @Test
    void skipsSpecialPoliciesForSessionsLongerThan24Hours() {
        AtomicInteger earlyBirdCalls = new AtomicInteger();
        AtomicInteger nightOwlCalls = new AtomicInteger();
        ParkingFeeCalculator calculator = new ParkingFeeCalculator(
                pricedAt(1000),
                session -> {
                    earlyBirdCalls.incrementAndGet();
                    return Optional.of(Money.ofCents(1));
                },
                session -> {
                    nightOwlCalls.incrementAndGet();
                    return Optional.of(Money.ofCents(1));
                });
        ParkingSession session = session(
                LoyaltyTier.NONE,
                DATE.atTime(10, 0),
                DATE.atTime(10, 0).plusHours(24).plusNanos(1));

        assertEquals(
                new Fare(Money.ofCents(1000), PolicyName.STANDARD_HOURLY),
                calculator.calculate(session));
        assertEquals(0, earlyBirdCalls.get());
        assertEquals(0, nightOwlCalls.get());
    }

    @Test
    void evaluatesSpecialPoliciesAtExactly24Hours() {
        AtomicInteger earlyBirdCalls = new AtomicInteger();
        AtomicInteger nightOwlCalls = new AtomicInteger();
        ParkingFeeCalculator calculator = new ParkingFeeCalculator(
                pricedAt(3000),
                countingPrice(earlyBirdCalls, 2000),
                countingPrice(nightOwlCalls, 1000));
        ParkingSession session = session(
                LoyaltyTier.NONE,
                DATE.atTime(10, 0),
                DATE.atTime(10, 0).plusHours(24));

        assertEquals(
                new Fare(Money.ofCents(1000), PolicyName.NIGHT_OWL),
                calculator.calculate(session));
        assertEquals(1, earlyBirdCalls.get());
        assertEquals(1, nightOwlCalls.get());
    }

    @Test
    void resolvesEqualFaresByNightOwlThenEarlyBirdThenStandard() {
        ParkingFeeCalculator calculator = new ParkingFeeCalculator(
                pricedAt(1000),
                pricedAt(1000),
                pricedAt(1000));
        ParkingSession session = session(
                LoyaltyTier.NONE,
                DATE.atTime(10, 0),
                DATE.atTime(10, 0).plusHours(24));

        assertEquals(
                new Fare(Money.ofCents(1000), PolicyName.NIGHT_OWL),
                calculator.calculate(session));
    }

    @Test
    void prefersEarlyBirdOverStandardWhenNightOwlIsNotAValidCandidate() {
        assertEquals(
                new Fare(Money.ofCents(1000), PolicyName.EARLY_BIRD),
                ParkingFeeCalculator.selectLowestFare(List.of(
                        new Fare(Money.ofCents(1000), PolicyName.STANDARD_HOURLY),
                        new Fare(Money.ofCents(1000), PolicyName.EARLY_BIRD))));
    }

    @Test
    void rejectsAStandardPolicyThatReturnsNoFare() {
        ParkingFeeCalculator calculator = new ParkingFeeCalculator(
                session -> Optional.empty(),
                pricedAt(1000),
                pricedAt(1000));
        ParkingSession session = session(
                LoyaltyTier.NONE,
                DATE.atTime(10, 0),
                DATE.atTime(11, 0));

        assertThrows(IllegalStateException.class, () -> calculator.calculate(session));
    }

    @Test
    void rejectsNullInputsAndEmptyCandidateCollections() {
        ParkingFeeCalculator calculator = calculator(Set.of());

        assertThrows(NullPointerException.class, () -> calculator.calculate(null));
        assertThrows(
                IllegalArgumentException.class,
                () -> ParkingFeeCalculator.selectLowestFare(List.of()));
        assertThrows(
                NullPointerException.class,
                () -> new ParkingFeeCalculator(null, pricedAt(0), pricedAt(0)));
    }

    private static ParkingFeeCalculator calculator(Set<LocalDate> holidays) {
        return new ParkingFeeCalculator(new FixedPublicHolidayCalendar(holidays));
    }

    private static RatePolicy pricedAt(long cents) {
        return session -> Optional.of(Money.ofCents(cents));
    }

    private static RatePolicy countingPrice(AtomicInteger calls, long cents) {
        return session -> {
            calls.incrementAndGet();
            return Optional.of(Money.ofCents(cents));
        };
    }

    private static ParkingSession session(
            LoyaltyTier loyaltyTier,
            LocalDateTime entry,
            LocalDateTime exit) {
        return new ParkingSession(
                new ParkingTicket(VehicleType.CAR, entry, loyaltyTier),
                exit);
    }
}
