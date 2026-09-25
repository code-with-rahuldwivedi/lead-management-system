# Admission Lead Management

**Edumerge Solutions — Pre-Drive Product Engineering Assignment**
**Assignment 5: Admission Lead Management**

| | |
|---|---|
| Candidate | Rahul Dwivedi |
| Submission date | 25 September 2026 |
| Repository |  https://github.com/code-with-rahuldwivedi/lead-management-system |

---

## Table of Contents

1. [Problem Statement](#1-problem-statement)
2. [Why This Assignment](#2-why-this-assignment)
3. [Tech Stack](#3-tech-stack)
4. [How to Run](#4-how-to-run)
5. [How to Test](#5-how-to-test)
6. [Project Structure](#6-project-structure)
7. [Architecture](#7-architecture)
8. [Domain Model](#8-domain-model)
9. [Key Design Decisions](#9-key-design-decisions)
10. [Assumptions](#10-assumptions)
11. [Trade-offs and What's Not Built](#11-trade-offs-and-whats-not-built)
12. [Edge Cases Covered](#12-edge-cases-covered)
13. [Manual Test Walkthrough](#13-manual-test-walkthrough)
14. [AI Usage](#14-ai-usage)

---

## 1. Problem Statement

An educational institution receives admission enquiries through multiple channels —
website, walk-ins, phone calls, WhatsApp, education fairs, and marketing campaigns.
Each enquiry ("lead") needs to be tracked from first contact through to admission or
loss, with a counsellor responsible for following up, and management needs visibility
into the overall pipeline.

The assignment brief deliberately left functional requirements, UI, data model,
technology choices, and architecture undefined, to be decided by the candidate. This
document explains the choices made and the reasoning behind them.

## 2. Why This Assignment

Five assignment options were offered: Smart Attendance Management, Fee Collection &
Reconciliation, Intelligent Timetable Generator, Student Support & Ticket Management,
and Admission Lead Management. This one was chosen because:

- It supports a complete, demonstrable lifecycle (enquiry → contact → conversion or
  loss) that can be built and validated end-to-end within the assignment window.
- It has clear opportunities to show product thinking — duplicate detection,
  ageing/stale leads, counsellor workload balancing — which the evaluation criteria
  specifically call out.
- It carries lower execution risk than the fee-reconciliation assignment (financial
  correctness bugs are costly) or the timetable generator (constraint-satisfaction
  logic is easy to get subtly wrong under time pressure).

## 3. Tech Stack

| Layer | Choice | Reason |
|---|---|---|
| Language | Java 17 | Required/preferred stack; LTS release |
| Framework | Spring Boot 3.5.7 | Mature, well-documented, fast to build CRUD + business rules |
| Web layer | Spring MVC + Thymeleaf | Server-rendered pages avoid a separate frontend project and its build tooling, within the time available |
| Persistence | Spring Data JPA + Hibernate | Standard ORM, reduces boilerplate SQL |
| Database | H2 (file-based) | Zero external setup — the evaluator can clone and run with no DB install |
| Validation | Jakarta Bean Validation | Declarative field-level validation on entities and DTOs |
| Testing | JUnit 5 + Spring Boot Test | In-memory H2 for isolated, repeatable test runs |
| Build | Maven | Standard, evaluator-friendly |

## 4. How to Run

```bash
git clone <repository-url>
cd lead-management
mvn clean spring-boot:run
```

- Application: <http://localhost:8080>
- H2 console: <http://localhost:8080/h2-console>
  - JDBC URL: `jdbc:h2:file:./data/leads`
  - Username: `sa`
  - Password: *(leave blank)*

On first startup, `DataSeeder` populates the database automatically (only if it's
empty) with:

- 6 courses
- 4 counsellors (3 active, 1 deliberately marked **unavailable** to demonstrate that
  inactive counsellors never receive leads)
- 40 leads with randomised but deterministic (fixed random seed) sources, statuses,
  creation dates, and follow-ups — so the dashboard and reports have realistic data
  to show immediately

To reset the demo data, stop the app and delete the `data/` folder before restarting.

## 5. How to Test

```bash
mvn test
```

20 automated tests run against an isolated in-memory H2 database (configured per test
class, independent of the file-based database the running app uses). They cover status
transition rules, duplicate detection, counsellor assignment, and follow-up validation
— see [Section 12](#12-edge-cases-covered) for the full list.

## 6. Project Structure

```
lead-management/
├── pom.xml
├── README.md
├── AI_USAGE_REPORT.md
└── src/
    ├── main/
    │   ├── java/com/example/leads/
    │   │   ├── LeadManagementApplication.java
    │   │   ├── model/            Entities and enums (Lead, Counsellor, Course,
    │   │   │                     FollowUp, ActivityLog, LeadStatus, LeadSource,
    │   │   │                     FollowUpType, ActivityType, AgeingBucket)
    │   │   ├── repository/       Spring Data JPA repositories + Specifications
    │   │   ├── service/          Business rules: LeadService, AssignmentService,
    │   │   │                     FollowUpService, ReportService
    │   │   ├── controller/       Thin HTTP controllers + global exception handler
    │   │   ├── dto/               Form-backing objects (LeadForm, CreateResult)
    │   │   ├── exception/        BusinessRuleException, ResourceNotFoundException
    │   │   ├── util/              PhoneUtil (phone normalisation)
    │   │   └── config/            DataSeeder
    │   └── resources/
    │       ├── application.properties
    │       ├── templates/        Thymeleaf pages (dashboard, leads, followups)
    │       └── static/css/       app.css
    └── test/
        └── java/com/example/leads/
            ├── model/             LeadStatusTest, AgeingBucketTest
            ├── util/               PhoneUtilTest
            └── service/           LeadServiceTest, FollowUpServiceTest
```

## 7. Architecture

```
Browser → Controller (HTTP binding only) → Service (business rules + audit log) → Repository (JPA) → H2
```

A strict, one-directional layering was used:

- **Controllers** parse request parameters, call exactly one service method, and pick
  a view or redirect. They contain **no business logic** — every `if` statement that
  decides whether an action is allowed lives in a service.
- **Services** own every business rule and write to the audit log inside the same
  transaction as the change itself, so a rule violation and its audit entry either
  both succeed or both roll back together.
- **Repositories** are Spring Data JPA interfaces, with a handful of `@Query` methods
  for aggregation (funnel counts, conversion by source) and a `Specification` for
  dynamic list filtering.

The reason for keeping business logic exclusively in the service layer: if a CSV bulk
import, a REST API, or an admin script were added later, they would call the same
`LeadService`/`FollowUpService`/`AssignmentService` methods and automatically inherit
every validation rule — there would be no way to create a lead or change its status
that bypasses the rules, because there is only one code path that does either.

## 8. Domain Model

| Entity | Purpose |
|---|---|
| `Lead` | Core record: name, phone, email, source, course interest, assigned counsellor, status, notes, timestamps |
| `Counsellor` | Staff member; `active` flag controls eligibility for new assignments |
| `Course` | Course the lead is interested in |
| `FollowUp` | A scheduled contact action (call, WhatsApp, email, campus visit) with due date and completion state |
| `ActivityLog` | Append-only audit entry — created on every lead creation, status change, (re)assignment, duplicate enquiry, and follow-up event |

**Lead status pipeline** (enforced by the `LeadStatus` enum's `allowedNext()` method):

```
NEW → CONTACTED → INTERESTED → APPLICATION_SUBMITTED → ADMITTED
  ↓        ↓            ↓                  ↓
 LOST     LOST         LOST               LOST
```

`ADMITTED` and `LOST` are terminal states — no further transitions are permitted from
either.

## 9. Key Design Decisions

**Status transitions live in one place.** The rules are defined once, on the
`LeadStatus` enum itself (`allowedNext()`, `canMoveTo()`), rather than scattered across
`if` statements in a controller or service method. This means the rule "a lead cannot
jump from New directly to Admitted" cannot be accidentally bypassed by any calling code.

**Duplicate detection has three layers, in order:**
1. *Normalisation* — `PhoneUtil` strips formatting so `+91 98765-43210`,
   `098765 43210`, and `9876543210` are recognised as the same number.
2. *Application check* — `LeadService.createLead()` looks up the normalised number
   before inserting; if found, it logs a "repeat enquiry" activity on the *existing*
   lead instead of creating a second row.
3. *Database constraint* — `phone` has a `UNIQUE` constraint as a last line of defence
   against a race condition (two requests arriving in the same instant), caught as a
   `DataIntegrityViolationException` and shown as a friendly message.

**Assignment is workload-based, not round-robin.** `AssignmentService.pickCounsellor()`
selects the *active* counsellor with the fewest currently *open* leads (closed leads
don't count against anyone), rather than a naive round-robin. This keeps the load fair
even as leads close at different rates for different counsellors.

**Counsellor unavailability triggers redistribution.** Marking a counsellor unavailable
(`AssignmentService.deactivateAndRedistribute()`) reassigns every one of their open
leads to the remaining active counsellors, recomputing the least-loaded counsellor for
each lead individually so the redistribution itself stays balanced. The system refuses
to deactivate the last remaining active counsellor.

**Ageing is based on contact, not edits.** `Lead.lastContactedAt` is updated only when
a human genuinely interacts with the lead (a status change or a completed follow-up),
and is kept separate from `updatedAt` (which changes on any field edit). This means the
"going cold" report reflects actual neglect, not just the record being untouched.

**Audit trail is append-only.** `ActivityLog` has no setters after construction and its
columns are marked `updatable = false`, so history cannot be silently rewritten.

**Optimistic locking.** `Lead` carries a `@Version` field, so if two staff members edit
the same lead concurrently, the second save fails with a clear conflict message instead
of silently overwriting the first change.

## 10. Assumptions

- No authentication/authorisation was in scope for this assignment; every action is
  attributed to a fixed actor string (`"admin"`) in the audit log. In a production
  system this would be replaced with the logged-in user's identity.
- Phone numbers are assumed to be Indian mobile numbers: 10 digits, starting with 6-9.
- Counsellor assignment is workload-based across *all* active counsellors; it is not
  filtered by course specialism (e.g. an MBA-focused counsellor could receive a
  B.Tech lead). This was a deliberate simplification — see trade-offs below.
- All timestamps use the server's local timezone (IST).
- "Manager visibility" was interpreted as a single dashboard rather than a separate
  login-gated manager role, since authentication was out of scope.

## 11. Trade-offs and What's Not Built

| Decision | Reasoning |
|---|---|
| Server-rendered Thymeleaf instead of a separate SPA/API | Fewer moving parts, faster to build, test, and demo within the assignment window |
| H2 file database instead of PostgreSQL/MySQL | Zero setup for the evaluator; swapping the datasource URL is the only change needed for a real deployment |
| No authentication/roles | Explicitly out of scope for the time available; noted as an assumption rather than silently ignored |
| No CSV bulk import | Would simulate website/fair bulk sources realistically, but the manual entry + auto-assignment + validation pipeline already demonstrates the same rules |
| No pagination on the lead list | Acceptable at demo data volume (40 leads); would be necessary at production scale |
| No email/WhatsApp notifications for follow-ups | Requires external service integration outside the assignment's scope |
| Assignment not filtered by course specialism | Kept the assignment algorithm simple and testable; a real system might weight by counsellor-course mapping |

## 12. Edge Cases Covered

Verified by automated tests (`LeadServiceTest`, `FollowUpServiceTest`,
`LeadStatusTest`, `PhoneUtilTest`, `AgeingBucketTest`):

- Illegal status jump (e.g. New → Admitted directly) is rejected
- Marking a lead Lost without a reason is rejected
- The same phone number in a different format (`+91 98765-43210` vs `9876543210`) is
  recognised as a duplicate, not a new lead
- Invalid phone number formats are rejected
- An inactive counsellor never receives a newly created lead (checked across multiple
  lead creations)
- A closed (Admitted/Lost) lead cannot be reassigned
- A lead cannot be reassigned to an inactive counsellor
- Marking a counsellor unavailable redistributes all of their open leads and the
  system refuses to deactivate the last active counsellor
- A follow-up cannot be scheduled in the past
- A follow-up cannot be scheduled on a closed lead
- The same follow-up cannot be marked complete twice
- Concurrent edits to the same lead are caught via optimistic locking
- Ageing bucket boundaries (0-2, 3-7, 8-14, 15+ days) are correct at the edges

## 13. Manual Test Walkthrough

A quick script for demonstrating the system live:

1. Open the dashboard — observe KPIs, funnel, ageing chart, source conversion table,
   and counsellor workload.
2. Create a new lead with phone `+91 98765-43210` — observe it gets auto-assigned to
   the least-loaded active counsellor.
3. Create another lead using the same number as `9876543210` — observe the warning
   that it's a repeat enquiry, and that no second lead is created.
4. Attempt to create a lead with an invalid phone (e.g. `12345`) — observe the
   validation error.
5. On a New lead, observe the status dropdown only offers *Contacted* and *Lost* (not
   Admitted).
6. Attempt to mark a lead Lost with an empty reason — observe the rejection.
7. Attempt to schedule a follow-up in the past — observe the rejection.
8. On the dashboard, mark an active counsellor unavailable — observe their open leads
   redistribute to the remaining counsellors.
9. Create a new lead and confirm it never goes to the now-inactive counsellor.
10. Open a lead's detail page and review its activity timeline.

## 14. AI Usage

See `AI_USAGE_REPORT.md` for the mandatory AI usage disclosure, including what was
asked of the AI tool, what was generated versus modified, and a documented case where
AI-generated output was wrong and how it was diagnosed and fixed.
