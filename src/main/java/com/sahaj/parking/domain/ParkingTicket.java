package com.sahaj.parking.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public record ParkingTicket(
        VehicleType vehicleType,
        LocalDateTime entryTimestamp,
        LoyaltyTier loyaltyTier) {

    public ParkingTicket {
        Objects.requireNonNull(vehicleType, "vehicleType");
        Objects.requireNonNull(entryTimestamp, "entryTimestamp");
        Objects.requireNonNull(loyaltyTier, "loyaltyTier");
    }
}
