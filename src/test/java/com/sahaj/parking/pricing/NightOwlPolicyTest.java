package com.sahaj.parking.pricing;

import com.sahaj.parking.domain.LoyaltyTier;
import com.sahaj.parking.domain.Money;
import com.sahaj.parking.domain.ParkingSession;
import com.sahaj.parking.domain.ParkingTicket;
import com.sahaj.parking.domain.VehicleType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NightOwlPolicyTest {
    private static final LocalDate ENTRY_DATE = LocalDate.of(2026, 11, 26);
    private final NightOwlPolicy policy = new NightOwlPolicy();

    @Test
    void includesEntryAndExitWindowStarts() {
        ParkingSession session = session(
                VehicleType.CAR,
                LoyaltyTier.NONE,
                ENTRY_DATE.atTime(18, 0),
                ENTRY_DATE.plusDays(1).atTime(5, 0));

        assertEquals(Money.ofCents(800), policy.calculate(session).orElseThrow());
    }

    @Test
    void acceptsTheLastRepresentableEntryTimeBeforeMidnight() {
        ParkingSession session = session(
                VehicleType.CAR,
                LoyaltyTier.NONE,
                ENTRY_DATE.atTime(LocalTime.MAX),
                ENTRY_DATE.plusDays(1).atTime(5, 0));

        assertEquals(Money.ofCents(800), policy.calculate(session).orElseThrow());
    }

    @Test
    void excludesEntryBefore18AndExitAtOrAfter10() {
        assertFalse(policy.calculate(session(
                VehicleType.CAR,
                LoyaltyTier.NONE,
                ENTRY_DATE.atTime(17, 59, 59, 999_999_999),
                ENTRY_DATE.plusDays(1).atTime(5, 0))).isPresent());
        assertFalse(policy.calculate(session(
                VehicleType.CAR,
                LoyaltyTier.NONE,
                ENTRY_DATE.atTime(18, 0),
                ENTRY_DATE.plusDays(1).atTime(10, 0))).isPresent());
    }

    @Test
    void rejectsExitBefore05OnTheNextDay() {
        assertFalse(policy.calculate(session(
                VehicleType.CAR,
                LoyaltyTier.NONE,
                ENTRY_DATE.atTime(18, 0),
                ENTRY_DATE.plusDays(1).atTime(4, 59, 59, 999_999_999))).isPresent());
    }

    @Test
    void requiresExitOnTheNextConsecutiveCalendarDay() {
        assertFalse(policy.calculate(session(
                VehicleType.CAR,
                LoyaltyTier.NONE,
                ENTRY_DATE.atTime(18, 0),
                ENTRY_DATE.plusDays(2).atTime(5, 0))).isPresent());
    }

    @Test
    void appliesLoyaltyDiscountAndVehicleMultiplierBeforeRounding() {
        ParkingSession session = session(
                VehicleType.BUS,
                LoyaltyTier.SILVER,
                ENTRY_DATE.atTime(18, 0),
                ENTRY_DATE.plusDays(1).atTime(5, 0));

        assertEquals(Money.ofCents(1440), policy.calculate(session).orElseThrow());
    }

    @Test
    void rejectsNullSession() {
        assertThrows(NullPointerException.class, () -> policy.calculate(null));
    }

    private static ParkingSession session(
            VehicleType vehicleType,
            LoyaltyTier loyaltyTier,
            LocalDateTime entry,
            LocalDateTime exit) {
        return new ParkingSession(
                new ParkingTicket(vehicleType, entry, loyaltyTier),
                exit);
    }
}
