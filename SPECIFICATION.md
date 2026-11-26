# Smart Parking Lot — Specification

## 1. Purpose

This specification defines the business rules, domain model, architectural boundaries,
calculation rules, assumptions, and acceptance criteria for the Smart Parking Lot
Rate Calculator.

The implementation is a Java billing engine with a small interactive console
adapter. It has no REST API, database, GUI, CLI framework, or external runtime
dependency.

## 2. Scope

### Included

- Vehicle classification
- Parking ticket entry time
- Loyalty tier storage
- Parking-duration calculation
- Standard Hourly pricing
- Peak-hour surcharge
- Early Bird pricing
- Night Owl pricing
- Vehicle-rate multipliers
- Loyalty discounts
- Minimum-fare conflict resolution
- Public-holiday-aware weekday peak pricing
- Exact monetary calculation
- Interactive console input and fare output
- Unit-testable domain rules

### Excluded

- Ticket issuance workflows
- Parking spot assignment
- Physical barrier or payment integration
- Customer identity management
- REST or other network APIs
- Database persistence
- Currency conversion
- Taxes
- Refunds or reversals
- Grace periods
- Subscription or membership processing
- Dynamic external pricing feeds

## 3. Functional Requirements

### 3.1 Vehicle Types

The system supports:

| Vehicle | Multiplier |
|---|---:|
| Motorcycle | 0.8x |
| Car | 1.0x |
| Bus | 2.0x |

The multiplier is applied to the derived car-based amount.

### 3.2 Loyalty Tiers

The system supports:

| Loyalty tier | Special-rate discount |
|---|---:|
| Silver | 10% |
| Gold | 20% |
| Platinum | 30% |

No-discount behavior is required for customers without a loyalty tier.

Loyalty discounts apply to the Early Bird and Night Owl flat car rates.

### 3.3 Parking Ticket

A parking ticket contains:

- A vehicle
- An entry timestamp
- A loyalty tier, using `NONE` when the customer has no loyalty membership

A ticket is an immutable domain record. It must not calculate or store the exit fee.

### 3.4 Parking Session

A parking session contains:

- A parking ticket
- An exit timestamp

The session is valid only when the exit timestamp is after the entry timestamp.

The system calculates elapsed duration from the exact entry and exit timestamps.

## 3A. Edge-Case and Negative-Input Matrix

| Edge case | Validation or result |
|---|---|
| Entry is after exit | Reject session construction with `IllegalArgumentException` |
| Entry is equal to exit | Reject as zero-duration; do not return a zero fee |
| Entry or exit is `null` | Reject with `NullPointerException` before pricing |
| Ticket is `null` | Reject with `NullPointerException` before session or policy evaluation |
| Vehicle is `null` | Reject with `NullPointerException` at ticket construction |
| Loyalty tier is `null` | Reject with `NullPointerException`; use explicit `NONE` where no loyalty discount applies |
| Unknown vehicle or tier enum value | Reject because only documented enum values are accepted |
| Public holiday calendar is `null` | Reject with `NullPointerException` when constructing its consumer |
| 1 minute duration | Standard rounds upward to 1 hour |
| 60 minutes duration | Standard charges 1 complete hour |
| 61 minutes duration | Standard rounds upward to 2 hours |
| Exactly 24 hours | Special policies remain eligible; no >24-hour override |
| 24 hours + 1 minute | Early Bird and Night Owl are invalid; Standard is exclusive |
| Entry 09:00 / exit 15:30 | Early Bird entry condition is already exclusive at 09:00; invalid |
| Entry 06:00 / exit 15:30 | Early Bird valid subject to same-day and non-over-24-hour rules |
| Exit exactly 19:00 | Early Bird invalid because exit end is exclusive |
| Entry exactly 18:00 | Night Owl valid subject to exit conditions |
| Night Owl entry at 23:59:59.999999999 | Valid entry time; entry at next-day 00:00 is not |
| Exit exactly 10:00 | Night Owl invalid because exit end is exclusive |
| Night Owl exit on skipped/nonconsecutive date | Invalid |
| Night Owl exit on next date before 05:00 | Invalid |
| Peak block ends exactly at peak start | No overlap |
| Peak block starts exactly at peak end | No overlap |
| Peak block starts exactly at peak start | Entire block overlaps |
| Hourly block has 1 minute in peak | Entire block is surcharged |
| Public holiday at peak time | No weekday peak surcharge |
| Weekend at peak time | No weekday peak surcharge |
| No applicable special policy | Standard-only result |
| Multiple policies have equal fee | Night Owl, then Early Bird, then Standard |
| A policy produces a negative amount | Reject policy data/arithmetical result |
| Loyalty discount is 100% or more | Reject unsupported loyalty configuration |

### 3A.1 Reversed and Equal Timestamps

`exitTimestamp.isBefore(entryTimestamp)` and
`exitTimestamp.isEqual(entryTimestamp)` are both invalid.

The first indicates that the exit occurred before entry.
The second indicates that no parking duration exists.

In both cases, a session must not be created, or the calculator must fail before
any policy executes. The system must not synthesize a fee of `$0.00`.

### 3A.2 Timestamp Precision

`LocalDateTime` is used for exact local timestamp values.

Nanoseconds and seconds remain available for duration calculation.
There is no implicit timezone conversion or daylight-saving normalization.

An implementation receiving timestamps from external systems must parse and
normalize the timezone before constructing the domain session.

## 4. Duration Rules

### 4.1 Standard-Policy Rounding

The Standard Hourly policy rounds elapsed duration upward to the nearest full hour.

Examples:

- 1 minute => 1 hour
- 60 minutes => 1 hour
- 61 minutes => 2 hours
- 2 hours 1 minute => 3 hours

Rounding must be applied consistently before progressive hourly rates are calculated.

### 4.2 Floating Hourly Blocks

The first Standard-policy hourly block begins at the exact entry timestamp.
Each subsequent block starts when the previous block ends.

Hourly blocks are not aligned to clock-hour boundaries.

For example, entry at 06:30:

- Hour 1: 06:30–07:30
- Hour 2: 07:30–08:30

### 4.3 Extended Stays

If elapsed duration is strictly greater than 24 hours, Early Bird and Night Owl
are automatically invalid. The Standard Hourly policy is evaluated exclusively.

Exactly 24 hours remains eligible for special-policy evaluation, subject to all
other applicability rules.

## 5. Standard Hourly Policy

The policy uses the following progressive car base rates:

| Hour number | Car hourly rate |
|---|---:|
| 1 | $5.00 |
| 2 | $3.00 |
| 3 and later | $2.00 |

Each rounded hourly block is treated as an individual calculation unit.

### 5.1 Peak Windows

Peak windows apply only on weekdays and not on public holidays.

| Window | Start | End |
|---|---|---|
| Morning | 07:00 | 10:00 |
| Evening | 16:00 | 19:00 |

Peak-window starts are inclusive. Peak-window ends are exclusive.

A weekday segment must pay the 1.5x surcharge if any part of that hourly segment
overlaps a peak window.

Partial overlap is sufficient to surcharge the entire one-hour block.

If a floating hourly block crosses midnight, assess its overlap with each local
calendar-date segment separately. A peak window applies only to the date on which
that window occurs, and only if that date is Monday through Friday and is not a
public holiday.

For example, entry 06:30 and exit 08:30 produces:

- Hour 1 from 06:30–07:30: $5.00 x 1.5 = $7.50
- Hour 2 from 07:30–08:30: $3.00 x 1.5 = $4.50
- Unadjusted car total: $12.00

### 5.2 Weekday Definition

Weekdays are Monday through Friday, excluding public holidays.

Public holidays are excluded from weekday peak surcharge eligibility.

The implementation does not treat a public holiday outside this set as a weekday
for surcharge purposes.

## 6. Early Bird Policy

Early Bird is valid only when all conditions hold:

- Entry is between 06:00 inclusive and 09:00 exclusive
- Exit is between 15:30 inclusive and 19:00 exclusive
- Entry and exit occur on the same calendar day
- Elapsed duration is no more than 24 hours

The base car flat rate is $15.00.

The applicable loyalty discount is subtracted from the flat car rate.

### Example

A Car enters at 06:30 and exits at 16:00:

- Base: $15.00
- Gold discount: 20%
- Final car amount: $12.00

## 7. Night Owl Policy

Night Owl is valid only when all conditions hold:

- Entry is at or after 18:00
- Entry occurs before midnight on the entry calendar day
- Exit is at or after 05:00 on the next consecutive calendar day
- Exit is before 10:00
- Entry and exit are on consecutive calendar days
- Elapsed duration is no more than 24 hours

The base car flat rate is $8.00.

The applicable loyalty discount is subtracted from the flat car rate.

## 8. Conflict Resolution

The calculator must:

1. Reject invalid parking sessions.
2. Evaluate all policies that are applicable.
3. Evaluate only Standard Hourly when elapsed duration exceeds 24 hours.
4. Calculate every applicable policy independently.
5. Select the smallest derived amount.
6. Return the fee and selected policy deterministically.

For equal fees, policy selection follows reverse policy priority:

1. Night Owl
2. Early Bird
3. Standard Hourly

This tie rule prevents nondeterministic results and is explicitly agreed for this
implementation.

The calculator owns the greater-than-24-hour gate: it skips both special policies
for those sessions. The special-policy implementations do not duplicate this
global gate.

## 9. Monetary Rules

- Currency is USD.
- All amounts are represented and calculated in integer cents.
- Java floating-point types must not be used for fare calculation.
- Round only at explicitly documented policy calculation boundaries.
- No currency conversion is required.
- Final amounts must never be negative.

### 9.1 Loyalty and Vehicle Order

For this implementation:

1. Start with the car-based policy amount.
2. Apply the loyalty discount for special flat rates.
3. Apply the vehicle multiplier.
4. Round the final cents to the nearest cent using `HALF_UP`.

This order is documented because the order can affect rounding for edge cases.

The Standard Hourly policy has no loyalty discount.

## 10. Public Holiday Source

The specification provides no public-holiday data source or complete holiday list.

The implementation will therefore use a fixed, configurable test calendar.

The fixed test calendar must:

- Be injectable rather than embedded in policy logic
- Use `java.time.LocalDate`
- Expose whether a date is a public holiday
- Be replaceable with a complete calendar in the future

The fixed test calendar is explicitly not represented as a comprehensive
production holiday source.

## 11. Assumptions

1. **Timezone:** Timestamps are `LocalDateTime` values supplied in a caller-defined,
   already-normalized timezone context. The engine performs no timezone conversion
   or daylight-saving adjustment.
2. **Public holidays:** Only dates in the configured test calendar are holidays.
   A real production holiday source is outside this assignment.
3. **Currency precision:** USD and integer cents are sufficient.
4. **Duration rounding:** Rounding applies to the Standard policy after the full
   elapsed duration is determined.
5. **No grace period:** Every minute or partial block is included in the calculation.
6. **Over-24-hour rule:** Strictly greater than 24 elapsed hours invalidates special
   policies. Exactly 24 hours does not, as described in section 4.3.
7. **Consecutive nights:** Night Owl requires adjacent calendar dates. Weekend,
   holiday, and one-minute boundary details do not alter consecutive-date checks.
8. **One-day early bird:** Early Bird requires entry and exit on the same
   calendar date.
9. **Vehicle multiplier timing:** The vehicle multiplier applies after the selected
   policy amount has been calculated.
10. **Special-rate discounts:** The same percentage structure applies to both
    Early Bird and Night Owl.
11. **No loyalty tier:** The engine uses explicit `NONE` and applies no special-rate
    discount. Loyalty tier is non-null in a valid ticket.
12. **Tie handling:** Night Owl wins ties, followed by Early Bird, then Standard
    Hourly.
13. **Invalid input:** Exit before or equal to entry is invalid.
14. **Currency rounding:** All derived cents are positive and are rounded with
    `HALF_UP` at the final policy-value stage.
15. **Mutability:** Domain objects are immutable except for explicitly mutable
    configuration objects such as the holiday calendar.
16. **Thread safety:** The calculator is stateless and reusable across threads.
17. **No persistence:** Tickets and sessions exist only in process memory.
18. **No external fees:** Taxes, reservation fees, and payment fees are excluded.

## 12. Domain Model

### 12.1 Core Types

- `VehicleType`
- `LoyaltyTier`
- `Money`
- `ParkingTicket`
- `ParkingSession`
- `RatePolicy`
- `ParkingFeeCalculator`
- `PublicHolidayCalendar`

### 12.2 Responsibilities

`VehicleType`:

- Defines the supported vehicle types
- Supplies the vehicle multiplier

`LoyaltyTier`:

- Defines the supported loyalty levels
- Supplies the special-rate discount

`Money`:

- Stores an exact integer-cent amount
- Prevents negative or fractional-cent construction
- Supports validated arithmetic

`ParkingTicket`:

- Stores the vehicle
- Stores the entry timestamp
- Stores the non-null loyalty tier; `NONE` represents no membership

`ParkingSession`:

- Stores a parking ticket
- Stores the exit timestamp
- Computes or exposes elapsed duration
- Does not perform pricing

`RatePolicy`:

- Determines whether it applies
- Calculates a derived amount
- Must not mutate session state

`ParkingFeeCalculator`:

- Applies the 24-hour policy override
- Evaluates valid policies
- Selects the minimum fee
- Returns the fee and selected policy

`PublicHolidayCalendar`:

- Answers whether a `LocalDate` is a public holiday
- Has no pricing responsibility

## 13. Architecture

The recommended structure separates domain data, policy rules, calendar rules,
and fee resolution:

```text
src/
  domain/
  pricing/
  calendar/
test/
  domain/
  pricing/
  calendar/
```

No database, framework, or dependency-injection container is required.

Policy objects are independent and receive the parking session and any shared
calculation services.

The fee calculator orchestrates policy evaluation but does not duplicate policy
applicability rules.

## 14. Validation and Security

The implementation must reject:

- Null vehicle values
- Null loyalty values
- Null tickets
- Null sessions
- Null timestamps
- Null holiday calendars
- Exit timestamps equal to or earlier than entry timestamps
- Unsupported enum values
- Negative monetary values

The engine has no external input or secret-handling surface. If added to an API
later, timestamp format, timezone, authorization, and authorization scope must
be validated before domain invocation.

## 15. Acceptance Criteria

The implementation is complete when:

- The Standard, Early Bird, and Night Owl rules pass boundary tests.
- Floating hourly blocks use the exact entry time.
- Partial peak overlap surcharges the entire block.
- Public holidays and weekends do not receive weekday peak surcharges.
- Special policies are disabled only when duration exceeds 24 hours.
- Vehicle multipliers are correct.
- Loyalty discounts are correct.
- Minimum-fare resolution is exhaustive and deterministic.
- All currency calculations use exact cents.
- Invalid sessions fail safely.
- The project builds and tests using documented standard commands.
- `README.md` describes project setup and final assumptions.
- `ai-chat-history.md` contains the complete chat history and prompts used.
