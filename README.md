# Smart Parking Lot Rate Calculator

A Java parking-fare engine with an interactive console for calculating the
lowest valid fare for a multi-level parking lot.

## Current Status

This repository contains the specification, architecture, diagrams, Java
domain model, three pricing policies, minimum-fare calculator, and an
interactive console application. The console is a thin adapter around the
in-process pricing engine.

JUnit 5 is a test-only dependency; production code uses only the Java standard
library.

## Requirements

- Java 21 or later
- Gradle wrapper

No application framework, database, REST API, or third-party runtime library
is required.

## Repository Documents

- [SPECIFICATION.md](SPECIFICATION.md) - business rules and acceptance criteria
- [ARCHITECTURE.md](ARCHITECTURE.md) - system design, component responsibilities,
  and conditional future enhancements
- [SYSTEM_DESIGN_DIAGRAMS.md](SYSTEM_DESIGN_DIAGRAMS.md) - editable Mermaid diagrams
- [diagrams/](diagrams/) - rendered PNG architecture diagrams
- [ai-chat-history.md](ai-chat-history.md) - AI assistance history

## Project Structure

Java source structure:

```text
src/
  main/java/com/sahaj/parking/
    Main.java
    domain/
    calendar/
    pricing/
  test/java/com/sahaj/parking/
    MainTest.java
    domain/
    calendar/
    pricing/
```

## Pricing Overview

The engine evaluates:

- Standard Hourly with weekday peak-hour surcharge
- Early Bird flat-rate special
- Night Owl flat-rate special

It returns the minimum valid fare. Stays strictly longer than 24 hours evaluate
only Standard Hourly.

### Monetary Precision

The implementation stores fares as integer cents. It does not use Java
floating-point types for fare calculation.

### Public Holidays

The specification provides no public-holiday source. The console accepts a
comma-separated list of ISO dates for the calculation; leave the prompt blank
when no holidays are configured. The engine receives these dates through its
fixed, immutable holiday calendar.

## Assumptions, Decisions, and Trade-offs

### Assumptions

- Entry and exit are caller-normalized local timestamps; the engine does no
  timezone or daylight-saving conversion.
- Exit must be strictly after entry. Standard Hourly rounds any partial hour
  upward; there is no grace period.
- Exactly 24 hours remains eligible for special rates; longer stays use
  Standard Hourly only.
- Early Bird requires same-day entry and exit. Night Owl requires exit on the
  next consecutive date.
- Peak windows are weekday-only, exclude configured holidays, and surcharge a
  whole floating hourly block on any positive overlap.
- Special rates receive loyalty discounts, then the vehicle multiplier.
  The same discount tiers apply to Early Bird and Night Owl. Standard Hourly
  receives the vehicle multiplier but no loyalty discount.
- Equal fares are resolved Night Owl, then Early Bird, then Standard Hourly.
- Currency is USD; taxes, external fees, refunds, and persistence are excluded.
- Tickets and sessions exist only in memory. The calculator and policies are
  stateless; the configured holiday calendar is immutable.
- No complete holiday source is specified. The console accepts a fixed set of
  holiday dates for each calculation; blank means no holidays configured.
- The domain uses `NONE` for customers without loyalty; a blank console response
  is converted to `NONE`.

### Design Decisions

- Java 21 and standard-library production code; Gradle builds the project and
  JUnit 5 is test-only.
- Immutable domain values represent tickets, sessions, fares, and money.
  `Money` stores integer cents and uses `BigDecimal` for exact percentage
  arithmetic with `HALF_UP` rounding.
- Standard Hourly, Early Bird, and Night Owl are independent policies.
  `ParkingFeeCalculator` evaluates applicable policies, applies the strict
  greater-than-24-hour gate, and selects the lowest fare.
- The console is a thin adapter; the pricing engine has no database, network
  service, or framework dependency.

### Trade-offs and Alternatives

- A caller-supplied holiday set is deterministic and avoids inventing a
  jurisdiction-specific calendar, but it is not a production holiday service.
- `LocalDateTime` keeps the engine platform-agnostic and simple; a caller must
  normalize timezones before calculation rather than relying on `ZonedDateTime`
  conversions inside pricing.
- Per-hour iteration makes floating-block peak behavior explicit and testable;
  work is proportional to billed hours, so no aggregation or caching is added
  without evidence it is needed.
- Separate policy objects add a few small types compared with one large
  conditional, in exchange for isolated rules and tests.
- Integer cents avoid floating-point fare errors; `BigDecimal` is retained only
  for exact intermediate percentage calculations rather than storing every
  amount as a decimal.
- A small console makes the engine runnable without a GUI, web API, database,
  or presentation framework, none of which is required for this assignment.

See [SPECIFICATION.md](SPECIFICATION.md) for detailed rules and edge cases, and
[ARCHITECTURE.md](ARCHITECTURE.md) for component responsibilities and design
details.

## Validation

The JUnit suite covers:

- Progressive hourly rates
- Floating hourly blocks
- Partial peak overlap
- Public holidays and weekends
- Early Bird boundaries
- Night Owl boundaries
- Loyalty discounts
- Vehicle multipliers
- 24-hour boundaries
- Reversed and equal timestamps
- Invalid null inputs
- Minimum-fare conflict resolution
- Tie priority

## Build and Test

Run the interactive calculator:

```powershell
.\gradlew.bat run
```

Enter the vehicle type, entry and exit local timestamps
(`yyyy-MM-ddTHH:mm`, without a timezone), and loyalty tier. Press Enter at the
loyalty prompt for `NONE`, or at the public-holiday prompt if there are no
holidays; otherwise enter comma-separated dates (`yyyy-MM-dd`). The console
prints the fare and selected policy. Invalid input is reported to stderr and
exits with a non-zero status.

Run the unit tests:

```powershell
.\gradlew.bat test
```

Build the project:

```powershell
.\gradlew.bat build
```

## Documentation and AI Transparency

The project documentation is separate from this README:

- Project rationale and assumptions are in `SPECIFICATION.md`.
- Architecture decisions are in `ARCHITECTURE.md`.
- User prompts, decisions, and phase summaries are in `ai-chat-history.md`.
- No custom agents or complex AI workflows were used.
