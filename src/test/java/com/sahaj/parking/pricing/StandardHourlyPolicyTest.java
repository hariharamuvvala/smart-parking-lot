package com.sahaj.parking.pricing;

import com.sahaj.parking.calendar.FixedPublicHolidayCalendar;
import com.sahaj.parking.domain.LoyaltyTier;
import com.sahaj.parking.domain.Money;
import com.sahaj.parking.domain.ParkingSession;
import com.sahaj.parking.domain.ParkingTicket;
import com.sahaj.parking.domain.VehicleType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StandardHourlyPolicyTest {
    private static final LocalDate WEEKDAY = LocalDate.of(2026, 11, 26);

    @Test
    void appliesPeakMultiplierToEachFloatingHourlyBlock() {
        StandardHourlyPolicy policy = policy(Set.of());
        ParkingSession session = session(
                VehicleType.CAR,
                WEEKDAY.atTime(6, 30),
                WEEKDAY.atTime(8, 30));

        assertEquals(Money.ofCents(1200), policy.calculate(session).orElseThrow());
    }

    @Test
    void chargesAFullHourWhenOnlyPartOfTheBlockOverlapsPeak() {
        StandardHourlyPolicy policy = policy(Set.of());
        ParkingSession session = session(
                VehicleType.CAR,
                WEEKDAY.atTime(6, 30),
                WEEKDAY.atTime(7, 0));

        assertEquals(Money.ofCents(750), policy.calculate(session).orElseThrow());
    }

    @Test
    void appliesProgressiveRatesAfterTheSecondHour() {
        StandardHourlyPolicy policy = policy(Set.of());
        ParkingSession session = session(
                VehicleType.CAR,
                WEEKDAY.atTime(10, 0),
                WEEKDAY.atTime(12, 1));

        assertEquals(Money.ofCents(1000), policy.calculate(session).orElseThrow());
    }

    @Test
    void doesNotSurchargeABlockBeginningAtPeakEnd() {
        StandardHourlyPolicy policy = policy(Set.of());
        ParkingSession session = session(
                VehicleType.CAR,
                WEEKDAY.atTime(10, 0),
                WEEKDAY.atTime(11, 0));

        assertEquals(Money.ofCents(500), policy.calculate(session).orElseThrow());
    }

    @Test
    void appliesEveningPeakSurcharge() {
        StandardHourlyPolicy policy = policy(Set.of());
        ParkingSession session = session(
                VehicleType.CAR,
                WEEKDAY.atTime(15, 30),
                WEEKDAY.atTime(16, 0));

        assertEquals(Money.ofCents(750), policy.calculate(session).orElseThrow());
    }

    @Test
    void excludesWeekendsAndConfiguredHolidaysFromPeakPricing() {
        LocalDate saturday = LocalDate.of(2026, 11, 28);
        StandardHourlyPolicy weekendPolicy = policy(Set.of());
        StandardHourlyPolicy holidayPolicy = policy(Set.of(WEEKDAY));

        assertEquals(
                Money.ofCents(800),
                weekendPolicy.calculate(session(
                        VehicleType.CAR,
                        saturday.atTime(6, 30),
                        saturday.atTime(8, 30))).orElseThrow());
        assertEquals(
                Money.ofCents(800),
                holidayPolicy.calculate(session(
                        VehicleType.CAR,
                        WEEKDAY.atTime(6, 30),
                        WEEKDAY.atTime(8, 30))).orElseThrow());
    }

    @Test
    void appliesVehicleMultiplierToTheCarTotal() {
        StandardHourlyPolicy policy = policy(Set.of());
        LocalDateTime entry = WEEKDAY.atTime(10, 0);

        assertEquals(
                Money.ofCents(400),
                policy.calculate(session(
                        VehicleType.MOTORCYCLE,
                        entry,
                        entry.plusHours(1))).orElseThrow());
        assertEquals(
                Money.ofCents(1000),
                policy.calculate(session(
                        VehicleType.BUS,
                        entry,
                        entry.plusHours(1))).orElseThrow());
    }

    @Test
    void rejectsNullInputs() {
        assertThrows(NullPointerException.class, () -> new StandardHourlyPolicy(null));
        assertThrows(
                NullPointerException.class,
                () -> policy(Set.of()).calculate(null));
    }

    private static StandardHourlyPolicy policy(Set<LocalDate> holidays) {
        return new StandardHourlyPolicy(new FixedPublicHolidayCalendar(holidays));
    }

    private static ParkingSession session(
            VehicleType vehicleType,
            LocalDateTime entry,
            LocalDateTime exit) {
        return new ParkingSession(
                new ParkingTicket(vehicleType, entry, LoyaltyTier.NONE),
                exit);
    }
}
