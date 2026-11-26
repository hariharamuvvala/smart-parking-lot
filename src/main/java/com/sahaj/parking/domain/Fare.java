package com.sahaj.parking.domain;

import java.util.Objects;

public record Fare(Money amount, PolicyName selectedPolicy) {
    public Fare {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(selectedPolicy, "selectedPolicy");
    }
}
