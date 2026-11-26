# Smart Parking Lot — System and Architectural Diagrams

## Rendered Image Gallery

The rendered PNG images are available in [diagrams/](diagrams/):

- [01-system-context.png](diagrams/01-system-context.png)
- [02-component-architecture.png](diagrams/02-component-architecture.png)
- [03-package-dependencies.png](diagrams/03-package-dependencies.png)
- [04-domain-model.png](diagrams/04-domain-model.png)
- [05-policy-contract.png](diagrams/05-policy-contract.png)
- [06-calculation-sequence.png](diagrams/06-calculation-sequence.png)
- [07-standard-hourly-flow.png](diagrams/07-standard-hourly-flow.png)
- [08-special-policy-applicability.png](diagrams/08-special-policy-applicability.png)
- [09-tie-break-flow.png](diagrams/09-tie-break-flow.png)
- [10-session-validation-state.png](diagrams/10-session-validation-state.png)
- [11-negative-case-map.png](diagrams/11-negative-case-map.png)
- [12-deployment-view.png](diagrams/12-deployment-view.png)
- [13-future-adapter-boundary.png](diagrams/13-future-adapter-boundary.png)

This document provides visual views of the system design. The diagrams describe
the in-process Java billing engine and its lightweight interactive console.
They remain independent of any database, framework, or network API.

## 1. System Context

```mermaid
flowchart LR
    Customer["Customer"]
    Operator["Parking Operator"]
    EntryExit["Entry / Exit Application"]
    Engine["Smart Parking Lot<br/>Rate Calculator"]
    HolidayConfig["Holiday Configuration"]

    Customer -->|"Entry and exit information"| EntryExit
    Operator -->|"Vehicle, ticket, loyalty input"| EntryExit
    EntryExit -->|"ParkingTicket and exit timestamp"| Engine
    HolidayConfig -->|"Public-holiday dates"| Engine
    Engine -->|"Final fare and selected policy"| EntryExit
    EntryExit -->|"Fare or ticket settlement result"| Customer
```

The implemented console is a thin local adapter. Physical entry/exit systems
and customer-facing applications remain outside the assignment scope.

## 2. High-Level Component Architecture

```mermaid
flowchart TB
    subgraph App["Application Boundary"]
        Main["Interactive Console (Main)"]
    end

    subgraph Engine["Pricing Engine"]
        Calculator["ParkingFeeCalculator"]
        Standard["StandardHourlyPolicy"]
        Early["EarlyBirdPolicy"]
        Night["NightOwlPolicy"]
    end

    subgraph Domain["Domain Layer"]
        Ticket["ParkingTicket"]
        Session["ParkingSession"]
        Vehicle["VehicleType"]
        Loyalty["LoyaltyTier"]
        Money["Money"]
        Fare["Fare"]
    end

    subgraph Calendar["Calendar and Time Layer"]
        Holiday["PublicHolidayCalendar"]
        Fixed["FixedPublicHolidayCalendar"]
        Window["TimeWindow"]
    end

    Main -->|"Create ticket and session"| Calculator
    Calculator --> Ticket
    Calculator --> Session
    Calculator --> Standard
    Calculator --> Early
    Calculator --> Night

    Standard --> Window
    Standard --> Holiday
    Standard --> Vehicle

    Early --> Vehicle
    Early --> Loyalty
    Night --> Vehicle
    Night --> Loyalty

    Fixed -. "implements" .-> Holiday
    Calculator -->|"creates"| Fare
    Fare --> Money
```

## 3. Package and Dependency View

```mermaid
flowchart LR
    subgraph domain["domain"]
        D1["VehicleType"]
        D2["LoyaltyTier"]
        D3["ParkingTicket"]
        D4["ParkingSession"]
        D5["Money"]
        D6["Fare"]
    end

    subgraph calendar["calendar"]
        C1["PublicHolidayCalendar"]
        C2["FixedPublicHolidayCalendar"]
        C3["TimeWindow"]
    end

    subgraph pricing["pricing"]
        P1["RatePolicy"]
        P2["StandardHourlyPolicy"]
        P3["EarlyBirdPolicy"]
        P4["NightOwlPolicy"]
        P5["ParkingFeeCalculator"]
    end

    subgraph app["application"]
        A1["Main"]
    end

    P2 --> P1
    P3 --> P1
    P4 --> P1
    P5 --> P1

    P5 --> D3
    P5 --> D4
    P5 --> D6
    P2 --> C1
    P2 --> C3
    P2 --> D5
    P3 --> D5
    P4 --> D5
    C2 -. "implements" .-> C1
    A1 --> P5
```

The arrows represent allowed production dependencies. Calendar and policy classes
must not depend on application or adapter classes.

## 4. Domain Object Model

```mermaid
classDiagram
    class VehicleType {
        <<enumeration>>
        MOTORCYCLE
        CAR
        BUS
        getMultiplier()
    }

    class LoyaltyTier {
        <<enumeration>>
        NONE
        SILVER
        GOLD
        PLATINUM
        getDiscountPercentage()
    }

    class Money {
        <<value object>>
        -long cents
        ofCents(long)
        plus(Money)
        multiply(BigDecimal)
        discount(BigDecimal)
    }

    class ParkingTicket {
        <<immutable>>
        -VehicleType vehicle
        -LocalDateTime entryTimestamp
        -LoyaltyTier loyaltyTier
    }

    class ParkingSession {
        <<immutable>>
        -ParkingTicket ticket
        -LocalDateTime exitTimestamp
        getElapsedDuration()
        isOver24Hours()
    }

    class PolicyName {
        <<enumeration>>
        STANDARD_HOURLY
        EARLY_BIRD
        NIGHT_OWL
    }

    class Fare {
        <<immutable>>
        -Money amount
        -PolicyName selectedPolicy
    }

    ParkingTicket --> VehicleType
    ParkingTicket --> LoyaltyTier
    ParkingSession --> ParkingTicket
    Fare --> Money
    Fare --> PolicyName
```

## 5. Policy Contract

```mermaid
classDiagram
    class RatePolicy {
        <<interface>>
        calculate(ParkingSession) Optional~Money~
    }

    class StandardHourlyPolicy {
        +calculate(ParkingSession) Optional~Money~
    }

    class EarlyBirdPolicy {
        +calculate(ParkingSession) Optional~Money~
    }

    class NightOwlPolicy {
        +calculate(ParkingSession) Optional~Money~
    }

    class PublicHolidayCalendar {
        <<interface>>
        isPublicHoliday(LocalDate) boolean
    }

    class TimeWindow {
        <<value object>>
        -LocalTime start
        -LocalTime end
        overlaps(LocalDateTime, LocalDateTime)
    }

    RatePolicy <|.. StandardHourlyPolicy
    RatePolicy <|.. EarlyBirdPolicy
    RatePolicy <|.. NightOwlPolicy
    StandardHourlyPolicy --> PublicHolidayCalendar
    StandardHourlyPolicy --> TimeWindow
```

An empty optional means that the policy is not applicable. An amount means that
the policy was valid and produced a fare candidate.

## 6. Minimum-Fare Calculation Sequence

```mermaid
sequenceDiagram
    actor Client
    participant Calculator as ParkingFeeCalculator
    participant Session as ParkingSession
    participant Standard as StandardHourlyPolicy
    participant Early as EarlyBirdPolicy
    participant Night as NightOwlPolicy

    Client->>Session: construct(ticket, exitTimestamp)
    Session->>Session: validate exit > entry
    Client->>Calculator: calculate(session)
    Calculator->>Session: getElapsedDuration()
    Calculator->>Standard: calculate(session)
    Standard-->>Calculator: Optional<Money>

    alt Duration <= 24 hours
        Calculator->>Early: calculate(session)
        Early-->>Calculator: Optional<Money>
        Calculator->>Night: calculate(session)
        Night-->>Calculator: Optional<Money>
    else Duration > 24 hours
        Note over Calculator, Night: Early Bird and Night Owl are skipped
    end

    Calculator->>Calculator: select minimum amount
    Calculator->>Calculator: resolve ties by priority
    Calculator-->>Client: Fare(amount, selectedPolicy)
```

## 7. Standard Hourly Calculation Flow

```mermaid
flowchart TD
    Start["Receive ParkingSession"]
    Validate["Validate session"]
    Duration["Calculate elapsed duration"]
    Round["Round upward to whole hours"]
    Blocks["Create floating hourly blocks<br/>starting at entry"]
    Loop{"Remaining blocks?"}
    Base["Get progressive car rate<br/>5, 3, then 2"]
    Weekday["Weekday and not public holiday?"]
    Peak["Block partially overlaps peak window?"]
    Surcharge["Multiply block by 1.5"]
    Sum["Add block amount"]
    Vehicle["Apply vehicle multiplier"]
    Result["Return exact cent amount"]

    Start --> Validate
    Validate --> Duration
    Duration --> Round
    Round --> Blocks
    Blocks --> Loop
    Loop -->|Yes| Base
    Base --> Weekday
    Weekday -->|Yes| Peak
    Weekday -->|No| Sum
    Peak -->|Yes| Surcharge
    Peak -->|No| Sum
    Surcharge --> Sum
    Sum --> Loop
    Loop -->|No| Vehicle
    Vehicle --> Result
```

Peak overlap uses:

```text
blockStart < peakWindowEnd
AND
blockEnd > peakWindowStart
```

This handles partial overlap and makes peak ends exclusive.

## 8. Special-Policy Applicability

```mermaid
flowchart TD
    Start["Receive ParkingSession"]
    Duration{"Duration <= 24 hours?"}
    Standard["Standard policy is always evaluated"]
    EarlyWindow{"Entry 06:00-09:00<br/>and exit 15:30-19:00?<br/>Same day?"}
    NightWindow{"Entry >= 18:00<br/>and exit next day 05:00-10:00?"}
    EarlyCandidate["Early Bird candidate: $15 car"]
    NightCandidate["Night Owl candidate: $8 car"]
    Skipped["Special policies skipped"]
    Discounts["Apply loyalty discount"]
    Multiplier["Apply vehicle multiplier"]
    Candidates["Add valid candidate(s)"]

    Start --> Duration
    Duration -->|Yes| Standard
    Duration -->|Yes| EarlyWindow
    Duration -->|Yes| NightWindow
    Duration -->|No| Skipped
    Skipped --> Standard
    EarlyWindow -->|Yes| EarlyCandidate
    NightWindow -->|Yes| NightCandidate
    EarlyCandidate --> Discounts
    NightCandidate --> Discounts
    Discounts --> Multiplier
    Multiplier --> Candidates
```

## 9. Tie-Break and Minimum-Fare Decision

```mermaid
flowchart TD
    Candidates["Collect applicable policy candidates"]
    Minimum["Find minimum exact-cent fare"]
    Tie{"More than one policy<br/>has the minimum?"}
    Night{"Night Owl candidate?"}
    Early{"Early Bird candidate?"}
    Standard["Select Standard Hourly"]
    ReturnNight["Select Night Owl"]
    ReturnEarly["Select Early Bird"]
    Fare["Return Fare"]

    Candidates --> Minimum
    Minimum --> Tie
    Tie -->|No| Fare
    Tie -->|Yes| Night
    Night -->|Yes| ReturnNight
    Night -->|No| Early
    Early -->|Yes| ReturnEarly
    Early -->|No| Standard
    ReturnNight --> Fare
    ReturnEarly --> Fare
    Standard --> Fare
```

This priority is intentionally documented and may be changed only through a
specification revision.

## 10. Session Validation State

```mermaid
stateDiagram-v2
    [*] --> InputReceived
    InputReceived --> InvalidNullInput: null ticket, timestamp, vehicle, loyalty, or calendar
    InputReceived --> InvalidTimestamp: exit <= entry
    InputReceived --> ValidSession: exit > entry
    InvalidNullInput --> [*]: throw validation exception
    InvalidTimestamp --> [*]: throw validation exception
    ValidSession --> Over24Hours: duration > 24 hours
    ValidSession --> Within24Hours: duration <= 24 hours
    Over24Hours --> StandardOnly
    Within24Hours --> MultiPolicyEvaluation
    MultiPolicyEvaluation --> FareSelected
    StandardOnly --> FareSelected
    FareSelected --> [*]
```

## 11. Negative-Case Decision Map

```mermaid
flowchart TD
    Input["Parking fee input"]
    Null{"Any required object or timestamp is null?"}
    Vehicle{"Vehicle or loyalty value invalid?"}
    Chronology{"Exit <= entry?"}
    Reject["Reject before policy evaluation"]
    Calculate["Calculate session"]

    Input --> Null
    Null -->|Yes| Reject
    Null -->|No| Vehicle
    Vehicle -->|Yes| Reject
    Vehicle -->|No| Chronology
    Chronology -->|Yes| Reject
    Chronology -->|No| Calculate
```

## 12. Deployment View

```mermaid
flowchart TB
    subgraph Runtime["Java Runtime"]
        Domain["Domain Classes"]
        Pricing["Pricing Engine"]
        Calendar["Holiday Calendar"]
        Entry["Optional Main / Future Adapter"]

        Entry --> Pricing
        Pricing --> Domain
        Pricing --> Calendar
    end

    subgraph External["External Resources"]
        HolidayData["Fixed Holiday Date Set"]
        TestRunner["JUnit Test Runner"]
    end

    HolidayData --> Calendar
    TestRunner --> Domain
    TestRunner --> Pricing
```

The runtime has no database, network listener, framework container, or external
service dependency.

## 13. Future Adapter Boundary

```mermaid
flowchart LR
    REST["Future REST Adapter"]
    CLI["Interactive Console (implemented)"]
    Persistence["Future Persistence Adapter"]

    DomainAPI["ParkingFeeCalculator"]

    REST -->|"validated DTO conversion"| DomainAPI
    CLI -->|"validated prompt input conversion"| DomainAPI
    Persistence -->|"ticket/session lookup"| DomainAPI

    DomainAPI -->|"Fare"| REST
    DomainAPI -->|"Fare"| CLI
    DomainAPI -->|"Fare"| Persistence
```

The interactive console is implemented as a thin adapter. REST and persistence
adapters remain future integration options.
