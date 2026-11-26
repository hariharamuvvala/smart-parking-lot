package com.sahaj.parking.pricing;

import com.sahaj.parking.domain.Money;
import com.sahaj.parking.domain.ParkingSession;

import java.util.Optional;

@FunctionalInterface
public interface RatePolicy {
    Optional<Money> calculate(ParkingSession session);
}
