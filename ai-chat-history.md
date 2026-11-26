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

### 10. Design Review Decisions

**User:** Requested a review of the spec and architecture before proceeding to
implementation.

**Assistant:** Identified unresolved details around:

- Test framework versus the standard-library-only constraint
- Night Owl's end-of-day timestamp interpretation
- Ownership of the greater-than-24-hour gate
- Peak blocks that cross midnight

**User decisions:**

- Use JUnit 5 as a test-only dependency; production code remains standard-library
  only.
- Use Gradle for build and test orchestration, with a committed wrapper.
- Treat Night Owl entry as any timestamp from 18:00 up to, but not including,
  midnight.
- Keep the greater-than-24-hour gate in the calculator, avoiding duplicate
  checks in each special policy.
- For peak blocks crossing midnight, check each date segment using that local
  date's weekday and holiday status.

**Assistant:** Recorded the decisions and began Phase 2 with Gradle project
scaffolding, immutable domain types, and focused unit tests. The initial JUnit
test run exposed a missing JUnit Platform launcher runtime dependency; adding
the aligned launcher dependency resolved discovery, and the domain tests passed
with the installed Gradle 8.14.3.

### 11. Repository Publication

**User:** Asked to create the repository and push the code.

**Assistant:** Verified that the GitHub repository and local Git repository did
not exist. The Java implementation had not yet been created, so the repository
was prepared for publication with the specification, architecture, diagrams,
README, and AI history.

### 12. Phase 2 — Domain Foundation

**User:** Asked what to do next, then selected a review of the specification and
architecture before implementation.

**Assistant:** Reviewed the documents and identified decisions to clarify:

- JUnit test dependency versus the standard-library-only constraint
- Night Owl's final entry-time boundary
- Ownership of the greater-than-24-hour rule
- Peak blocks spanning midnight

**User decisions:** JUnit 5 is acceptable as a test-only dependency; Night Owl
entry is permitted from 18:00 up to but not including midnight; the calculator
owns and enforces the over-24-hour gate; peak blocks crossing midnight are
checked by local date segment, using each date's weekday and holiday status.
Gradle was selected for build and test orchestration.

**Assistant:** Updated the specification, architecture, and README to record
those decisions. Implemented the Gradle project skeleton and immutable domain
types for vehicle, loyalty tier, money, ticket, parking session, fare, and policy
name. Added JUnit tests for domain values, validation, exact elapsed duration,
and the 24-hour boundary.

The first JUnit run exposed an unaligned JUnit Platform launcher. Adding the
launcher as a test runtime dependency fixed test discovery. A clean local
Gradle test run passed all 12 tests, and `gradle build` succeeded.

Gradle wrapper files were generated. Running the wrapper could not download the
Gradle distribution because connections to `services.gradle.org` timed out;
validation therefore used the installed Gradle 8.14.3.

### 13. Credit Tracking

**User:** Reported usage of 21.7 out of 200 credits.

**Assistant:** Calculated this as 10.85% used, below the requested 60% alert
threshold of 120 credits. At that report, 98.3 additional credits would reach
the threshold.

### 14. Latest Update

**User:** Asked to push the latest changes and update this chat history file.

**Assistant:** Updated the prompt log and pushed the Gradle project, domain
foundation, tests, documentation decisions, and wrapper files in commit
`2958a0d`.

### 15. Phase 3 — Calendar and Shared Time Rules

**User:** `thanks for the confirmation, go ahead next`

**Assistant:** Added immutable fixed-holiday lookup, weekday-excluding-holiday
rules, date-aware time-window overlap, and duration ceiling to whole hours.
Added tests for holiday configuration and defensive copying, weekday/weekend
behavior, inclusive/exclusive window overlap, cross-midnight intervals, and
duration rounding boundaries. `gradle clean test` passed; `gradle build`
succeeded. The suite contains 24 tests with no failures or errors.

### 16. Phase 4 — Pricing Policies

**User:** `go ahead next`

**Assistant:** Implemented the `RatePolicy` contract and the Standard Hourly,
Early Bird, and Night Owl policies. Added tests for progressive hourly rates,
peak boundaries and partial overlap, weekends and holidays, vehicle multipliers,
loyalty discounts, special-rate eligibility windows, and next-day conditions.
`gradle clean test` and `gradle build` passed; the suite contains 45 tests with
no failures or errors. The minimum-fare calculator remains for the next phase.

## User Prompt Log

The user prompts are reproduced below as captured in the conversation (with
line wrapping for readability). The two assignment PDFs are referenced by
filename; their full text is in the supplied attachments.

1. `[Assignment_Instructions.pdf](file:///d%3A/Smart%20Parking%20lot/Assignment_Instructions.pdf)
   [The_Smart_Parking_Lot_v2.pdf](file:///d%3A/Smart%20Parking%20lot/The_Smart_Parking_Lot_v2.pdf)
   This is Smart Parking Lot assignment. As Software architect this need to
   built based on the spec and instructions given. First understand documents,
   come up with the plan. This should consider spec, test cases, system design,
   architecture and domain objects. Consdier performance security aspects as
   well. Before starting the plan. If any questions ask now.`
2. `Before I read the plan, I want to give few more instructions .`
3. `1) All the chat histroy has to be checked in and shared with project. Readme
   should be spricific to project. THis should be sepearete from readme file
   2) Push this code into github. This is new repo. We need to create as well
   3) Keep montior context and credits, if it cross 60% of utilization let me know.`
4. `Before starting, Lets do step by step rather doing everything one shot.
   Provide plan for approach`
5. `This looks like, you understand the spec, statrted implemntation phases.
   Lets have Phase 0, where dive the spec, in next phase provide system design`
6. `Lets creat spec document for this. Provide the assumptions you have consider`
7. `I have gone throguh spec. Looks correct, as we make progress, lets revisit
   and see if any modifications.`
8. `Lets understand tie-break sceneraion.`
9. `For now, this tie break make sense which is priortiy based`
10. `Correct this, it should be in the reverse order`
11. `Write in chat as comment. One example ti-break has shown here. for the sake
    of dicscussion, accepted all asumptions. In theroy, we have to go through
    every piece and adjust the spec.`
12. `Provide the system/archtiectural design`
13. `Stucturally looks good, with tech and data flow, negative testing, failure
    cases. Lets deep dive on edge case, like entry time is after the exit time`
14. `Can you create system/archtiectural design daigrams`
15. `as images`
16. `where is this chat file`
17. `create repo and push the code`
18. `whats next`
19. `21.7 of 200 credits are used`
20. `push the latest changes and update the chat history file`
21. `thanks for the confirmation, go ahead next`
22. `go ahead next`
23. `Go ahead`
24. `lets complete the code first`
25. The user reported that the console rejected an empty loyalty-tier value.
26. The user reported an invalid session because the exit time was not after entry.
27. `success,`
28. `next please`
29. `we should add future enhancements thinking as architect`
30. `create presentation`

### Structured Decisions

- Language: Java
- Public holiday source: fixed test calendar
- GitHub: public `hariharamuvvala/smart-parking-lot`
- Chat history filename: `ai-chat-history.md`
- Tie priority: Night Owl, Early Bird, Standard Hourly
- JUnit 5: test-only dependency
- Gradle: build and test orchestration
- Night Owl entry: 18:00 inclusive up to, but not including, midnight
- Over-24-hour gate: owned by the calculator
- Peak blocks crossing midnight: evaluate each local date segment against that
  date's weekday and holiday status
- Credit update: 21.7 of 200 used; alert threshold 60%
- User selected an interactive console application as the final code scope

### 17. Phase 5 — Best-Value Calculator

**User:** `Go ahead`

**Assistant:** Added `ParkingFeeCalculator` to evaluate Standard Hourly for
every valid session, skip Early Bird and Night Owl when duration exceeds 24
hours, evaluate applicable special policies otherwise, and return the lowest
fare with the approved Night Owl > Early Bird > Standard tie priority. Added
calculator tests for minimum selection, tie priority, the 24-hour gate,
inapplicable policies, and failure behavior. `gradle clean test` and
`gradle build` passed; the suite contains 54 tests with no failures or errors.

### 18. Phase 6 — Interactive Console

**User:** `lets complete the code first`

**User (scope confirmation):** `interactive_cli`

**Assistant:** Added an interactive console entry point with vehicle, timestamp,
loyalty, and optional public-holiday inputs. It reports the selected fare and
policy, validates malformed input and invalid sessions, and uses the existing
pricing engine. Updated the run instructions and architecture/specification
documents to reflect the console boundary. `gradle clean test` and
`gradle build` passed; the suite contains 58 tests with no failures or errors.
The Gradle `run` task was also exercised with a sample input and returned the
expected `$5.00` Standard Hourly fare. After the user reported an empty loyalty
input, the console was updated to map a blank loyalty response to `NONE`, with
the prompt, README, and integration test updated accordingly. The user later
reported an exit timestamp validation error; clarified that exit must be
strictly after entry and recommended plain Gradle console output. The user
confirmed the run succeeded.

### 19. Final Acceptance Check

**User:** `next please`

**Assistant:** Compared the implementation and documentation against the
assignment PDFs. Re-ran `gradle clean test` and `gradle build`; all 58 tests
passed with no failures or errors. The review found that the assignment asks
README.md itself to cover assumptions, decisions, trade-offs, and alternatives,
so those summaries were added. Verified the clean working tree and pushed state.

### 20. Conditional Future Enhancements

**User:** `we should add future enhancements thinking as architect`

**Assistant:** Added a conditional architecture roadmap covering authoritative
holiday/rate configuration, external integration boundaries, audit and
persistence, multi-location timezone handling, observability and measured
performance optimization, and adjacent capabilities kept outside the pricing
engine. These are future options, not current implementation commitments.

### 21. Architecture Presentation

**User:** `create presentation`

**Assistant:** Created a 10-slide PowerPoint architecture review deck covering
business requirements, system and domain architecture, pricing policies,
minimum-fare selection, edge cases, quality attributes, test evidence, and the
conditional future roadmap. Linked the deck from README.md.

## Transparency Statement

AI assistance was used to analyze the documents, formulate assumptions,
identify edge cases, and produce specifications and diagrams. Final repository
implementation, testing, and publication decisions remain subject to explicit
human review and repository verification.
