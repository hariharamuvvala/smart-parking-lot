package com.sahaj.parking.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ParkingTicketTest {
    private static final LocalDateTime ENTRY = LocalDateTime.of(2026, 11, 26, 6, 30);

    @Test
    void requiresVehicleEntryTimestampAndLoyaltyTier() {
        assertThrows(
                NullPointerException.class,
                () -> new ParkingTicket(null, ENTRY, LoyaltyTier.NONE));
        assertThrows(
                NullPointerException.class,
                () -> new ParkingTicket(VehicleType.CAR, null, LoyaltyTier.NONE));
        assertThrows(
                NullPointerException.class,
                () -> new ParkingTicket(VehicleType.CAR, ENTRY, null));
    }
}
