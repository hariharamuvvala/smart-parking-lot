package com.sahaj.parking.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParkingSessionTest {
    private static final LocalDateTime ENTRY = LocalDateTime.of(2026, 11, 26, 6, 30);

    @Test
    void rejectsExitBeforeEntry() {
        ParkingTicket ticket = ticketAt(ENTRY);

        assertThrows(
                IllegalArgumentException.class,
                () -> new ParkingSession(ticket, ENTRY.minusNanos(1)));
    }

    @Test
    void rejectsExitEqualToEntry() {
        ParkingTicket ticket = ticketAt(ENTRY);

        assertThrows(IllegalArgumentException.class, () -> new ParkingSession(ticket, ENTRY));
    }

    @Test
    void exposesExactElapsedDuration() {
        ParkingSession session = new ParkingSession(ticketAt(ENTRY), ENTRY.plusMinutes(61));

        assertEquals(Duration.ofMinutes(61), session.duration());
    }

    @Test
    void exactly24HoursDoesNotExceedSpecialRateLimit() {
        ParkingSession session = new ParkingSession(ticketAt(ENTRY), ENTRY.plusHours(24));

        assertFalse(session.exceeds24Hours());
    }

    @Test
    void durationOneNanosecondOver24HoursExceedsSpecialRateLimit() {
        ParkingSession session = new ParkingSession(
                ticketAt(ENTRY),
                ENTRY.plusHours(24).plusNanos(1));

        assertTrue(session.exceeds24Hours());
    }

    private static ParkingTicket ticketAt(LocalDateTime entryTimestamp) {
        return new ParkingTicket(VehicleType.CAR, entryTimestamp, LoyaltyTier.NONE);
    }
}
