package com.sahaj.parking.pricing;

import com.sahaj.parking.calendar.PublicHolidayCalendar;
import com.sahaj.parking.domain.Fare;
import com.sahaj.parking.domain.Money;
import com.sahaj.parking.domain.ParkingSession;
import com.sahaj.parking.domain.PolicyName;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ParkingFeeCalculator {
    private final RatePolicy standardHourlyPolicy;
    private final RatePolicy earlyBirdPolicy;
    private final RatePolicy nightOwlPolicy;

    public ParkingFeeCalculator(PublicHolidayCalendar holidayCalendar) {
        this(
                new StandardHourlyPolicy(holidayCalendar),
                new EarlyBirdPolicy(),
                new NightOwlPolicy());
    }

    ParkingFeeCalculator(
            RatePolicy standardHourlyPolicy,
            RatePolicy earlyBirdPolicy,
            RatePolicy nightOwlPolicy) {
        this.standardHourlyPolicy =
                Objects.requireNonNull(standardHourlyPolicy, "standardHourlyPolicy");
        this.earlyBirdPolicy = Objects.requireNonNull(earlyBirdPolicy, "earlyBirdPolicy");
        this.nightOwlPolicy = Objects.requireNonNull(nightOwlPolicy, "nightOwlPolicy");
    }

    public Fare calculate(ParkingSession session) {
        Objects.requireNonNull(session, "session");

        List<Fare> candidates = new ArrayList<>(3);
        Money standardAmount = standardHourlyPolicy.calculate(session)
                .orElseThrow(() -> new IllegalStateException(
                        "Standard hourly policy must always produce a fare"));
        candidates.add(new Fare(standardAmount, PolicyName.STANDARD_HOURLY));

        if (!session.exceeds24Hours()) {
            addCandidate(candidates, earlyBirdPolicy, session, PolicyName.EARLY_BIRD);
            addCandidate(candidates, nightOwlPolicy, session, PolicyName.NIGHT_OWL);
        }

        return selectLowestFare(candidates);
    }

    private static void addCandidate(
            List<Fare> candidates,
            RatePolicy policy,
            ParkingSession session,
            PolicyName policyName) {
        Optional<Money> amount = policy.calculate(session);
        amount.ifPresent(money -> candidates.add(new Fare(money, policyName)));
    }

    static Fare selectLowestFare(List<Fare> candidates) {
        Objects.requireNonNull(candidates, "candidates");
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("At least one fare candidate is required");
        }

        return candidates.stream()
                .min(Comparator.comparingLong((Fare fare) -> fare.amount().cents())
                        .thenComparingInt(fare -> tieBreakPriority(fare.selectedPolicy())))
                .orElseThrow();
    }

    private static int tieBreakPriority(PolicyName policyName) {
        return switch (policyName) {
            case NIGHT_OWL -> 0;
            case EARLY_BIRD -> 1;
            case STANDARD_HOURLY -> 2;
        };
    }
}
