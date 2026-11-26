# Smart Parking Lot Rate Calculator

A Java domain engine for calculating the lowest valid parking fare for a
multi-level parking lot.

## Current Status

This repository currently contains the completed specification, architectural
design, Mermaid source diagrams, rendered PNG diagrams, assumptions, and AI
interaction history.

The Java implementation and JUnit test suite have not been added yet.

## Requirements

- Java 21 or later
- Git
- GitHub CLI for repository operations

No application framework, database, REST API, or third-party runtime library is
required.

## Repository Documents

- [SPECIFICATION.md](SPECIFICATION.md) - business rules and acceptance criteria
- [ARCHITECTURE.md](ARCHITECTURE.md) - system design and component responsibilities
- [SYSTEM_DESIGN_DIAGRAMS.md](SYSTEM_DESIGN_DIAGRAMS.md) - editable Mermaid diagrams
- [diagrams/](diagrams/) - rendered PNG architecture diagrams
- [ai-chat-history.md](ai-chat-history.md) - AI assistance history

## Project Structure

Planned Java source structure:

```text
src/
  com/
    sahaj/
      parking/
        domain/
        calendar/
        pricing/

test/
  com/
    sahaj/
      parking/
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

The implementation will store fares as integer cents. It will not use Java
floating-point types for fare calculation.

### Public Holidays

The specification provides no public-holiday source. A fixed, configurable
`LocalDate` set will be used for the assignment and tests.

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

## Planned Validation

The JUnit suite will cover:

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

Build and test commands will be added after the Java implementation is created.

## Documentation and AI Transparency

The project documentation is separate from this README:

- Project rationale and assumptions are in `SPECIFICATION.md`.
- Architecture decisions are in `ARCHITECTURE.md`.
- The complete AI interaction history is in `ai-chat-history.md`.
