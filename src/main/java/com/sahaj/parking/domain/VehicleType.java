package com.sahaj.parking.domain;

import java.math.BigDecimal;

public enum VehicleType {
    MOTORCYCLE("0.8"),
    CAR("1.0"),
    BUS("2.0");

    private final BigDecimal rateMultiplier;

    VehicleType(String rateMultiplier) {
        this.rateMultiplier = new BigDecimal(rateMultiplier);
    }

    public BigDecimal rateMultiplier() {
        return rateMultiplier;
    }
}
