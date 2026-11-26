# AI Interaction History

This file records the AI-assisted design and implementation discussion for the
Smart Parking Lot assignment.

## 2026-11-26

### 1. Initial Assignment Review

**User:** Supplied the `Assignment_Instructions.pdf` and
`The_Smart_Parking_Lot_v2.pdf`, and asked for a software-architecture plan based
on the specifications, tests, system design, architecture, domain objects,
performance, and security.

**Assistant:** Reviewed the documents and requested practical decisions about
public-holiday data and implementation language.

### 2. Technology Decisions

**User:** Selected Java and a standard-library-plus-fixed-test-calendar approach
for public holidays.

**Assistant:** Created an implementation plan covering domain objects, policies,
testing, documentation, security, validation, and GitHub publication.

### 3. Repository Decisions

**User:** Requested:

- Full chat history checked into the project
- Chat history in a file separate from `README.md`
- GitHub publication to a new public repository
- Repository: `hariharamuvvala/smart-parking-lot`
- Chat history format: `ai-chat-history.md`
- Notification if context utilization reaches 60%

**User-reported decision:** Proceed step by step rather than one-shot.

**Assistant:** Adopted phased delivery and confirmed checkpoints.

### 4. Specification Phase

**User:** Asked for a specification and assumptions.

**Assistant:** Created `SPECIFICATION.md`, including:

- Scope and exclusions
- Vehicle and loyalty rules
- Ticket and session domain behavior
- Duration rounding
- Floating hourly blocks
- Standard peak and surcharge rules
- Early Bird rules
- Night Owl rules
- Conflict resolution
- Currency rules
- Public-holiday assumptions
- Negative and invalid input behavior
- Domain model and acceptance criteria

### 5. Tie-Break Discussion

**User:** First discussed priority-based tie handling.

**Assistant:** Initially recorded Standard, Early Bird, then Night Owl priority
for ties.

**User:** Corrected this to reverse priority.

**User-confirmed final tie priority:**

1. Night Owl
2. Early Bird
3. Standard Hourly

**Assistant:** Updated `SPECIFICATION.md` to reflect the reverse priority.

**User clarification:** The tie-break example is for discussion and all
assumptions are accepted provisionally. Every specification piece should still
be checked during implementation.

### 6. Architectural Design

**User:** Asked for system/architectural design.

**Assistant:** Created `ARCHITECTURE.md`, covering:

- System goals
- Layered in-process architecture
- Package structure
- Domain responsibilities
- `RatePolicy` contract
- Standard, Early Bird, and Night Owl policy design
- Holiday and time-window services
- Calculator orchestration
- Data flow
- Exact-cent monetary design
- SOLID trade-offs
- Performance
- Security
- Concurrency
- Extensibility
- Testing architecture
- Failure behavior

### 7. Edge-Case Deep Dive

**User:** Asked to deep dive into edge cases, including entry after exit.

**Assistant:** Added an edge-case matrix and validation behavior to
`SPECIFICATION.md` and `ARCHITECTURE.md`, including:

- Exit before entry
- Exit equal to entry
- Null values
- Invalid vehicles and loyalty tiers
- Exactly 24 hours
- 24 hours plus one minute
- Boundary inclusivity
- Consecutive-night requirements
- Public holidays
- Weekends
- Negative fare and discount conditions
- Reverse tie priority

Confirmed behavior:

- Entry after exit is invalid.
- Entry equal to exit is invalid.
- Invalid sessions are rejected before policy evaluation.
- The engine must not synthesize a `$0.00` fare for invalid input.

### 8. Architectural Diagrams

**User:** Asked for system and architectural design diagrams.

**Assistant:** Created `SYSTEM_DESIGN_DIAGRAMS.md` with Mermaid models for:

- System context
- Component architecture
- Package dependencies
- Domain model
- Policy contract
- Minimum-fare sequence
- Standard calculation flow
- Special-policy applicability
- Tie-break flow
- Session validation state
- Negative-case map
- Deployment view
- Future adapter boundary

**Assistant:** Extracted the Mermaid diagrams and rendered 13 PNG images in
`diagrams/`. Mermaid source files were placed in `diagrams-source/`.

### 9. AI History File

**User:** Asked for the location of the chat history file.

**Result:** No chat-history file existed at that point. This
`ai-chat-history.md` file was created to satisfy the assignment requirement.

### 10. Repository Publication

**User:** Asked to create the repository and push the code.

**Assistant:** Verified that the GitHub repository and local Git repository did
not exist. The Java implementation had not yet been created, so the repository
was prepared for publication with the specification, architecture, diagrams,
README, and AI history.

## Transparency Statement

AI assistance was used to analyze the documents, formulate assumptions,
identify edge cases, and produce specifications and diagrams. Final repository
implementation, testing, and publication decisions remain subject to explicit
human review and repository verification.
