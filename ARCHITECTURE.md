# Smart Parking Lot — System and Architectural Design

For editable Mermaid models, see [SYSTEM_DESIGN_DIAGRAMS.md](SYSTEM_DESIGN_DIAGRAMS.md).
Rendered PNG images are available in [diagrams/](diagrams/).

## 1. Goals

The architecture is designed to deliver:

- Correct and deterministic fare calculation
- Clear separation of domain state and behavior
- Independent, testable pricing policies
- Exact monetary arithmetic
- Small implementation surface consistent with KISS and YAGNI
- Platform-agnostic domain concepts implemented in Java
- No frameworks, database, REST API, or external pricing dependencies

The design prioritizes maintainability and test confidence over presentation,
configuration frameworks, or premature infrastructure abstractions.

## 2. Architectural Style

The system uses a simple modular, in-process domain architecture:

```text
Entry Point
    |
    v
Fee Calculation Orchestrator
    |
    +--> Standard Hourly Policy
    +--> Early Bird Policy
    +--> Night Owl Policy
    |
    v
Domain Objects and Shared Calendar/Time Services
```

The architecture is layered conceptually:

1. **Domain layer**: state and domain invariants
2. **Pricing layer**: policy applicability and calculation
3. **Calendar/time layer**: weekday, holiday, and time-window rules
4. **Application entry point**: validates interactive input and invokes the engine
5. **Test layer**: policy, boundary, and integration tests

There is no persistence or network transport layer. The console is a thin
application adapter. If an API or database is introduced later, it should adapt
to this engine rather than becoming a domain dependency.

## 3. Package Structure

```text
src/
  main/
    java/
      com/
        sahaj/
          parking/
            Main.java
            domain/
            calendar/
            pricing/
  test/
    java/
      com/
        sahaj/
          parking/
            domain/
            calendar/
            pricing/
```

Package names are illustrative for Java and may be adjusted if repository naming
conventions require it.

## 4. Domain Model

### 4.1 VehicleType

`VehicleType` is an immutable enum:

- `MOTORCYCLE`
- `CAR`
- `BUS`

It owns or exposes the vehicle's car-rate multiplier.

This is appropriate because vehicle classification and its multiplier are domain
rules that belong together.

### 4.2 LoyaltyTier

`LoyaltyTier` is an immutable enum:

- `NONE`
- `SILVER`
- `GOLD`
- `PLATINUM`

It exposes the special-rate discount percentage.

No-loyalty behavior is represented explicitly by `NONE`, avoiding nullable loyalty
state in the domain.

### 4.3 Money

`Money` is an immutable value object storing integer cents.

Responsibilities:

- Prevent negative fare construction
- Prevent unsupported fractional-cent values
- Provide validated arithmetic
- Apply percentage and vehicle calculations in exact integer terms
- Format for display when necessary

`BigDecimal` may be used during parsing or intermediate exact arithmetic where
needed, but the accepted amount stored in `Money` is cents. Java `double` and
`float` must not be used for fares.

### 4.4 ParkingTicket

`ParkingTicket` is an immutable domain object containing:

- Vehicle
- Entry `LocalDateTime`
- Loyalty tier

It validates null and unsupported values at construction.

It deliberately does not store a fee, because fee calculation depends on exit
time and applicable policies.

### 4.5 ParkingSession

`ParkingSession` is an immutable object containing:

- Parking ticket
- Exit `LocalDateTime`

Responsibilities:

- Validate that exit is after entry
- Expose the elapsed duration
- Serve as the context passed to policies

It should not calculate fares or mutate ticket state.

### 4.6 Fare

`Fare` is an immutable calculation result containing:

- Exact final `Money`
- Selected `PolicyName`

The result is useful for:

- Verifying the minimum-fare decision
- Testing deterministic policy selection
- Returning a human-readable policy name
- Future auditing or API integration

### 4.7 PolicyName

`PolicyName` is an enum:

- `STANDARD_HOURLY`
- `EARLY_BIRD`
- `NIGHT_OWL`

It is metadata; policy names must not control pricing logic.

## 5. Pricing Contracts

### 5.1 RatePolicy

```text
interface RatePolicy
    Optional<Money> calculate(ParkingSession session)
```

The method contract means:

- `Optional.empty()` means the policy is not applicable
- A present `Money` value means the policy is valid and has a derived amount
- The policy does not select the overall winner
- The policy does not mutate session or ticket state

This contract keeps the calculator independent of policy-specific conditionals.

## 6. Policy Design

### 6.1 StandardHourlyPolicy

Responsibilities:

- Round elapsed duration upward to whole hours
- Build floating hourly blocks from the exact entry timestamp
- Determine progressive base rates
- Check weekday/public-holiday/peak overlap
- Apply the 1.5x peak surcharge
- Apply the vehicle multiplier

Shared rules should be extracted only when they improve clarity:

- Hourly duration rounding
- Floating block creation
- Time-window overlap
- Peak weekday validation

This policy remains responsible for its hourly and peak-specific behavior.

### 6.2 EarlyBirdPolicy

Responsibilities:

- Confirm entry and exit windows
- Confirm same-calendar-day eligibility
- Calculate the $15.00 car flat rate
- Apply the loyalty discount
- Apply the vehicle multiplier

### 6.3 NightOwlPolicy

Responsibilities:

- Confirm entry and exit windows
- Confirm consecutive-calendar-day eligibility
- Calculate the $8.00 car flat rate
- Apply the loyalty discount
- Apply the vehicle multiplier

### 6.4 Policy Independence

No policy calls or compares another policy. This avoids orchestration logic
inside policy implementations and makes every policy independently testable.

## 7. Shared Time and Calendar Services

### 7.1 PublicHolidayCalendar

```text
interface PublicHolidayCalendar
    boolean isPublicHoliday(LocalDate date)
```

The interface is intentionally small. It separates holiday lookup from pricing
and permits:

- Fixed test calendar
- External future configuration
- A larger future calendar implementation

No framework or dependency injection container is required.

### 7.2 FixedPublicHolidayCalendar

The initial implementation stores an explicit `Set<LocalDate>`.

It is:

- Deterministic
- Replaceable
- Testable
- Suitable for the no-database constraint
- Immutable after construction

The default test set must be identified as illustrative, not a comprehensive
holiday list.

### 7.3 TimeWindow

`TimeWindow` is an immutable value object for an inclusive start and exclusive
end.

It can answer whether a one-hour block overlaps the window.

The window applies to a specified local calendar date. A caller evaluating a
block that crosses midnight checks each date segment against that date's window
and weekday/holiday status.

Peak-window detection must use a positive intersection rule:

```text
blockStart < windowEnd && blockEnd > windowStart
```

This naturally implements:

- Peak start inclusive
- Peak end exclusive
- Partial overlap surcharge
- No overlap for touching boundaries

If a floating hourly block crosses midnight, split the block at midnight and
check each date segment against that date's weekday and holiday status. Each
peak window belongs to its own local calendar date.

## 8. Calculator Orchestration

`ParkingFeeCalculator` owns the global conflict-resolution algorithm.

It must:

1. Validate or receive a valid `ParkingSession`
2. Determine whether elapsed duration exceeds 24 hours
3. Evaluate Standard Hourly
4. If duration is 24 hours or less, evaluate Early Bird and Night Owl
5. Ignore inapplicable policies
6. Compare exact cent values
7. Select the minimum fare
8. Resolve ties using the approved priority

The calculator is the single owner of the greater-than-24-hour gate. When a
session exceeds 24 hours, it skips Early Bird and Night Owl. The special policies
do not repeat this global check.

The approved tie priority is:

1. Night Owl
2. Early Bird
3. Standard Hourly

## 9. Calculation Data Flow

```text
ParkingTicket
    |
    +-- Vehicle
    +-- Entry timestamp
    +-- Loyalty tier
    |
    v
ParkingSession(ticket, exitTimestamp)
    |
    v
ParkingFeeCalculator
    |
    +-- elapsed duration check
    +-- StandardHourlyPolicy.calculate(session)
    +-- if <=24h: EarlyBirdPolicy.calculate(session)
    +-- if <=24h: NightOwlPolicy.calculate(session)
    |
    v
minimum result + deterministic tie priority
    |
    v
Fare
```

The session is not mutated during calculation.

## 10. Monetary and Rounding Design

### 10.1 Representation

The base unit is cents:

- `$5.00` = `500`
- `$3.00` = `300`
- `$15.00` = `1500`
- `$8.00` = `800`

The implementation must not use `double` or `float` for fare calculations.

### 10.2 Calculation Order

For Standard:

1. Sum exact base hourly cents
2. Apply 1.5x per overlapping block
3. Apply the vehicle multiplier
4. Store the exact rounded cent amount

For Early Bird and Night Owl:

1. Start with the flat car cents
2. Apply the loyalty discount
3. Apply the vehicle multiplier
4. Round the final value to cents using `HALF_UP`

The documented order should be tested because round timing can affect edge cases.

## 11. SOLID Applied Pragmatically

### Single Responsibility

- Domain objects own state and validation
- Policies own policy-specific applicability and pricing
- Calculator owns conflict resolution
- Holiday calendar owns holiday lookup

### Open/Closed

Adding a new policy should require:

- A policy implementation
- A small registration or calculation list update

It should not require rewriting existing policy logic.

### Liskov Substitution

Every `RatePolicy` implementation follows the same optional-result contract.

### Interface Segregation

Interfaces are small:

- `RatePolicy`: one calculation method
- `PublicHolidayCalendar`: one holiday lookup method

No broad service interface is introduced.

### Dependency Inversion

Policies and the calculator depend on domain concepts and small abstractions,
not on framework, database, or transport types.

## 12. Performance

The engine is computationally bounded:

- At most three policies are evaluated
- Standard hourly calculation is proportional to the number of rounded hours
- Long sessions may use a large number of hourly segments
- No I/O or persistence is in the hot path
- A shared calculator instance is stateless and reusable

For unusually long sessions, the Standard policy will iterate each hourly block.

The implementation may optimize repeated second-and-later hours only if profiling
or test clarity shows a need. It should not obscure the floating-block calculation
or add cache complexity prematurely.

## 13. Security

The engine itself has no authentication, network, or secret-handling surface.

Defensive boundaries include:

- Null validation
- Exit-after-entry validation
- Enum validation
- Exact cents
- No mutable calculation state
- Stateless calculator
- No external I/O

At the interactive console boundary:

- Validate timestamp syntax and require caller-normalized local times
- Validate vehicle and loyalty strings before enum conversion
- Treat calculation inputs as untrusted
- Report invalid input without exposing stack traces

## 14. Concurrency

The calculator and policy objects should be stateless or contain only immutable
configuration.

The fixed holiday calendar should use an immutable or thread-safe set.

The design supports concurrent calls without locks or shared mutable session
state.

## 15. Extensibility

The design can grow to support:

- A new rate policy
- A new loyalty tier
- A production holiday calendar
- Parking session reporting
- Persistence adapters
- HTTP or richer CLI adapters

The following should remain unchanged when those adapters are added:

- Domain objects
- `RatePolicy` contract
- Exact fare calculation
- Minimum-fare rule

Adapters should translate external input and output rather than implement pricing.

## 16. Trade-offs

### Java Standard Library

Java `LocalDateTime` and `Duration` provide clear calendar and duration behavior
without external dependencies.

### Integer Cents

Integer cents offer exact arithmetic but require careful conversion of discounts
and multipliers.

### Small Interfaces

Small interfaces avoid unnecessary framework abstractions and make unit testing
straightforward.

### Fixed Holiday Set

A fixed set satisfies the assignment and keeps the project reproducible. It does
not attempt to model future public-holiday legislation or external sources.

### No API or Database

The assignment is an engine problem. Omitting APIs and persistence follows YAGNI
and avoids heavy frameworks.

### Explicit Policy Objects

Policy objects add slightly more classes but make applicability rules isolated,
readable, and testable.

## 17. Testing Architecture

Tests should be organized around behavior:

- Domain tests
- Calendar/window tests
- Policy tests
- Conflict-resolution tests

Preferred tests:

- Table-driven boundary tests
- Exact-cent assertions
- Fixed-holiday lookup, defensive copying, and weekday exclusion
- Time-window edge overlap, touching boundaries, and midnight crossing
- Upward rounding at exact, fractional, and sub-hour duration boundaries
- Independent policy applicability tests
- Calculator integration tests
- Reversed timestamp tests
- Peak blocks crossing midnight with weekday and holiday checks per date
- Night Owl entry at the final representable time before midnight and at midnight
- No-mutation tests

The test suite uses JUnit 5 as a test-only dependency. JUnit is not needed at
runtime; production code uses the Java standard library only.

The project uses Gradle for build and test orchestration. The Gradle wrapper
should be committed so builds are reproducible without requiring a globally
installed Gradle version.

## 18. Failure Behavior

The domain should reject invalid state before pricing begins.

The preferred failure types are:

- `IllegalArgumentException` for invalid domain values
- `NullPointerException` for a null object where nullability is prohibited

Input validation should be performed at construction boundaries:

```text
ParkingTicket
    |
    +-- reject null vehicle
    +-- reject null entry timestamp
    +-- reject null loyalty tier; use NONE for no loyalty

ParkingSession
    |
    +-- reject null ticket
    +-- reject null exit timestamp
    +-- reject exit <= entry

ParkingFeeCalculator
    |
    +-- reject null session
    +-- reject null calendar/policy dependencies
```

The validation boundary is the constructor or first public calculation boundary.
It must not depend on a policy silently validating the overall session.

The engine should not:

- Silently return `$0.00`
- Return a negative fare
- Return a policy for an inapplicable session
- Use an arbitrary tie result
- Silently change timezone or round durations twice

## 19. Architectural Acceptance Criteria

The design is complete when:

- Every class has one clear responsibility
- The three policies are independently instantiable and testable
- The calculator never duplicates applicability logic
- Every fare is represented in cents
- Holiday lookup is isolated from pricing
- Session and ticket objects do not mutate during calculation
- The tie priority is deterministic
- Over-24-hour sessions evaluate only Standard Hourly
- Tests can run without external services or data
