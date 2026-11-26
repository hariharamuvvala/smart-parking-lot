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
- [ARCHITECTURE.md](ARCHITECTURE.md) - system design and component responsibilities
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

## Assumptions

- Timestamps use a caller-normalized local timezone context.
- The engine performs no timezone conversion.
- Exit timestamps must be after entry timestamps.
- Exit equal to entry is invalid.
- Exactly 24 hours remains eligible for special policies.
- Stays strictly longer than 24 hours disable Early Bird and Night Owl.
- Peak, Early Bird, and Night Owl boundaries use the inclusivity rules in
  `SPECIFICATION.md`.
- Reverse tie priority is Night Owl, Early Bird, then Standard Hourly.
- Public holidays are supplied only by the fixed test calendar.

See [SPECIFICATION.md](SPECIFICATION.md) for the complete assumptions and edge
cases.

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
(`yyyy-MM-ddTHH:mm`, without a timezone), loyalty tier, and optional
comma-separated public-holiday dates (`yyyy-MM-dd`). The console prints the fare
and selected policy. Invalid input is reported to stderr and exits with a
non-zero status.

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
- The complete AI interaction history is in `ai-chat-history.md`.
