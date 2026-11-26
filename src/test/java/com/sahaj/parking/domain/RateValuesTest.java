package com.sahaj.parking.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RateValuesTest {
    @Test
    void exposesVehicleMultipliersFromSpecification() {
        assertEquals(new BigDecimal("0.8"), VehicleType.MOTORCYCLE.rateMultiplier());
        assertEquals(new BigDecimal("1.0"), VehicleType.CAR.rateMultiplier());
        assertEquals(new BigDecimal("2.0"), VehicleType.BUS.rateMultiplier());
    }

    @Test
    void exposesLoyaltyDiscountsFromSpecification() {
        assertEquals(new BigDecimal("0.00"), LoyaltyTier.NONE.discountRate());
        assertEquals(new BigDecimal("0.10"), LoyaltyTier.SILVER.discountRate());
        assertEquals(new BigDecimal("0.20"), LoyaltyTier.GOLD.discountRate());
        assertEquals(new BigDecimal("0.30"), LoyaltyTier.PLATINUM.discountRate());
    }
}
