package com.sahaj.parking.pricing;

import com.sahaj.parking.domain.LoyaltyTier;
import com.sahaj.parking.domain.Money;
import com.sahaj.parking.domain.ParkingSession;
import com.sahaj.parking.domain.ParkingTicket;
import com.sahaj.parking.domain.VehicleType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EarlyBirdPolicyTest {
    private static final LocalDate DATE = LocalDate.of(2026, 11, 26);
    private final EarlyBirdPolicy policy = new EarlyBirdPolicy();

    @Test
    void includesEntryAndExitWindowStarts() {
        ParkingSession session = session(
                VehicleType.CAR,
                LoyaltyTier.NONE,
                DATE.atTime(6, 0),
                DATE.atTime(15, 30));

        assertEquals(Money.ofCents(1500), policy.calculate(session).orElseThrow());
    }

    @Test
    void excludesEntryAndExitWindowEnds() {
        assertFalse(policy.calculate(session(
                VehicleType.CAR,
                LoyaltyTier.NONE,
                DATE.atTime(9, 0),
                DATE.atTime(16, 0))).isPresent());
        assertFalse(policy.calculate(session(
                VehicleType.CAR,
                LoyaltyTier.NONE,
                DATE.atTime(8, 0),
                DATE.atTime(19, 0))).isPresent());
    }

    @Test
    void rejectsTimesBeforeTheEntryOrExitWindow() {
        assertFalse(policy.calculate(session(
                VehicleType.CAR,
                LoyaltyTier.NONE,
                DATE.atTime(5, 59, 59, 999_999_999),
                DATE.atTime(16, 0))).isPresent());
        assertFalse(policy.calculate(session(
                VehicleType.CAR,
                LoyaltyTier.NONE,
                DATE.atTime(8, 0),
                DATE.atTime(15, 29, 59, 999_999_999))).isPresent());
    }

    @Test
    void requiresEntryAndExitOnTheSameCalendarDay() {
        assertFalse(policy.calculate(session(
                VehicleType.CAR,
                LoyaltyTier.NONE,
                DATE.atTime(8, 0),
                DATE.plusDays(1).atTime(16, 0))).isPresent());
    }

    @Test
    void appliesLoyaltyDiscountAndVehicleMultiplierBeforeRounding() {
        ParkingSession session = session(
                VehicleType.MOTORCYCLE,
                LoyaltyTier.GOLD,
                DATE.atTime(8, 0),
                DATE.atTime(16, 0));

        assertEquals(Money.ofCents(960), policy.calculate(session).orElseThrow());
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
