package com.sahaj.parking.domain;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

public record ParkingSession(ParkingTicket ticket, LocalDateTime exitTimestamp) {
    private static final Duration MAX_SPECIAL_RATE_DURATION = Duration.ofHours(24);

    public ParkingSession {
        Objects.requireNonNull(ticket, "ticket");
        Objects.requireNonNull(exitTimestamp, "exitTimestamp");

        if (!exitTimestamp.isAfter(ticket.entryTimestamp())) {
            throw new IllegalArgumentException("Exit timestamp must be after entry timestamp");
        }
    }

    public Duration duration() {
        return Duration.between(ticket.entryTimestamp(), exitTimestamp);
    }

    public boolean exceeds24Hours() {
        return duration().compareTo(MAX_SPECIAL_RATE_DURATION) > 0;
    }
}
