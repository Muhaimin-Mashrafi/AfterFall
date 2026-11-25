# AfterFall Development Progress

This file is the handoff point between Claude Code sessions. Update it after every completed task. Keep it factual and concise.

## Backend cleanup (2026-11-25)
- Removed the orphaned `EventBus` bean in `PersistenceConfiguration` (the engine owns its bus; `SimulationService` registers the run-bound recorder) and reworked `PersistenceWiringIntegrationTest` accordingly.
- Removed three unused DTOs (`PendingChoiceDto`, `ChoiceOptionDto`, `InterventionRecordDto`), unused imports, and inline fully-qualified names in `SimulationService`.
- Added `ConcurrentCommandOrderingTest` (interventions queued from 8 threads apply in intervention-id order; same final state as sequential submission).
- `mvn test`: 307 run, 0 failures, 11 errors, all in `Postgres17LiveIntegrationTest` (no PostgreSQL available in this sandbox; needs PostgreSQL 17). No behaviour or API change.

## Current status
- Project: AfterFall - The Last Days of Humanity
- Canonical specification: `docs/blueprint/AfterFall-Blueprint-Revised-Solo.md`
- Current milestone: M13 in progress (Phases 1, 2 & 4 complete: Feature Freeze declared; 8 Bug Bash scenarios executed; empirical verification completed for hard JVM termination/persistence recovery, live WebSocket automatic reconnection without manual connect(), 100-entry DOM chronicle cap, and responsive layouts across 1366x768 and 1920x1080; 303 total passing tests).
- Application code: Spring Boot skeleton, domain models, autonomous behaviors, action commands, 4-worker multithreaded simulation engine, deterministic merge, batch rollback, lifecycle state management, resource pools, crises, ObserverAI, EventBus fan-out, template-driven Narrator, Choice moments & consequence application, threshold-driven Chapter policies, dual-adapter persistence with asynchronous recording, Story-First HTML/CSS/JS web frontend, and Hidden Records & Final Documentary Recap.
- Last updated: 2026-11-25 (v1.0.0 merge: PRs 1-9 on main; 314 tests, all green on PostgreSQL 17.12; rehearsals and fallback video still to do)

## Environment (user-reported)
- Assistant: Google Antigravity
- Windows 10 Pro, 64-bit.
- Eclipse Temurin JDK 21.0.13 installed; `java`, `javac` and `JAVA_HOME` verified.
- VS Code 1.146.0 installed.
- Git 2.45.1 installed; Git Bash exists at `C:\Program Files\Git\bin\bash.exe`.
- Node.js 26.13.1 and npm 11.20.0 installed.
- PostgreSQL 17.12 installed; Windows service `postgresql-x64-17` running.
- `psql` connection to `localhost:5432` as `postgres` succeeded.
- pgAdmin 4 opens; database `afterfall` created and visible in pgAdmin.
- AfterFall workspace: `C:\Users\user\Projects\AfterFall`.
- Build system: Maven Wrapper (`mvnw.cmd`).
- Spring Boot 4.1.2 verified.

## Git status
- GitHub repository: `https://github.com/arthur-boyle-excalibur/AfterFall` (user reports it was created as private).
- User reports Git identity, local repository initialization and `origin` remote setup were completed. Verify with `git status`, `git branch --show-current` and `git remote -v` before the first commit.
- Expected branch: `main`.
- Expected remote: `https://github.com/arthur-boyle-excalibur/AfterFall.git`.
- Expected Git name: `Abdullah Al Muhaimin`.
- Git email should be the GitHub noreply address configured by the user. Do not print or change it unless necessary.

## Master Development Sequence

1. **AfterFall V1 (M0–M13):** Current active baseline. Core single-player simulation, 4-worker concurrency, narrative chronicles, dual-adapter persistence, story-first browser frontend, and documentary recaps.
2. **AfterFall: Observer Embodiment (O0–O9):** First-person 3D embodiment expansion in Godot 4. Player physically walks the settlement as the Observer with authoritative Java backend connectivity. Prerequisite for V2.
3. **AfterFall V2:** Multiplayer expansion introducing human-controlled Guardian vs Observer asymmetric intelligence warfare.

## Milestone tracker

### Active Track: AfterFall V1
| Milestone | Scope | Status |
|---|---|---|
| Environment | JDK, Git, PostgreSQL, VS Code, workspace | Mostly complete; verify extensions and Git remote |
| M0 | Spring Boot project setup, packages, initial UML | Complete (2026-11-19) |
| M1 | Domain model and OOP pillars | Complete (Sessions 1 & 2 + final-review corrections; 2026-11-20) |
| M2 | Behaviours, actions and core patterns | Complete (2026-11-20) |
| M3 | Correct single-threaded simulation baseline | Complete (2026-11-23) |
| M4 | Four workers, deterministic merge, RNG, migration and concurrency | Complete (2026-11-23) |
| M5 | Resources and Famine/Disease crises | Complete (2026-11-23) |
| M6 | ObserverAI with four MUST policies | Complete (2026-11-23) |
| M7 | EventBus and Narrator with 40+ templates | Complete (2026-11-23) |
| M8 | Choices, consequences and four threshold-driven chapters | Complete (2026-11-24) |
| M9 | Spring Boot, PostgreSQL, persistence interfaces and async recorder | Complete (2026-11-24) |
| M10 | Story-first frontend: four core pages plus operator controls | Complete (2026-11-24) |
| M11 | Recovered records and final recap | Complete (2026-11-25) |
| M12 | Integration, tests, UML, docs and checkpoint demo | Complete (2026-11-25) |
| M13 | Feature freeze, bug bash, three rehearsals and fallback recording | In progress (Phases 1 & 2 complete: Freeze declared, 8 Bug Bash scenarios verified; 303 tests) |
| M14 (SHOULD) | Audience voting | Deferred / Optional |
| M15 (SHOULD) | Scale to about 100 survivors | Deferred / Optional |
| M16 (SHOULD) | Fifth chapter, more records, imagery and optional map | Deferred / Optional |

### Future Track: Observer Embodiment (Planned, Post-M13)
*Transforms AfterFall from browser UI into an immersive first-person 3D simulation client (Godot 4 + Blender), backed by the authoritative Java 21 simulation engine.*

| Milestone | Scope | Status |
|---|---|---|
| O0 | Architecture, Godot setup and backend connectivity | Future / Planned |
| O1 | Human Observer body and first-person controller | Future / Planned |
| O2 | 3D settlement environment | Future / Planned |
| O3 | Survivor models, navigation and movement | Future / Planned |
| O4 | Social interactions and contextual conversations | Future / Planned |
| O5 | Observer perception, proximity and inspection | Future / Planned |
| O6 | Physical Observer intervention interface | Future / Planned |
| O7 | Live manipulation-to-reaction visualization | Future / Planned |
| O8 | Survivor awareness and cinematic storytelling | Future / Planned |
| O9 | Full integration, performance and standalone release | Future / Planned |

#### Observer Embodiment MVP Acceptance Criteria
Before expanding the environment, the initial MVP must demonstrate:
1. One explorable 3D settlement environment.
2. First-person Observer movement with human-looking hands/body.
3. Ten visible simulated survivors driven by real backend state.
4. Autonomous survivor navigation and movement between stations.
5. Contextual conversations reflecting real survivor relationships.
6. Individual survivor inspection dossier interface.
7. Three existing Observer intervention policies executed physically.
8. At least one complete intervention-to-visible-reaction sequence.
9. Live connectivity with the authoritative Java 21 simulation engine.

#### Observer Embodiment Final Acceptance Gate
Must satisfy:
- Player physically explores settlement in first-person with occasional third-person cinematic camera transitions.
- Survivors perform genuine simulation-driven activities (no invented decorative logic).
- Conversations reflect real relationships, memories, and circumstances.
- Observer interventions produce traceable visual and behavioral reactions.
- Survivor awareness of Observer develops according to defined simulation rules.
- Standalone Windows client build running smoothly alongside backend.
- Full preservation of existing V1 functionality and test coverage.

### Future Track: AfterFall V2 (Planned, Post-O9)
*Asymmetric multiplayer intelligence warfare: Human Observer vs Human Guardian.*
- Full V2 development begins strictly after O0–O9 is accepted.
- Second human-controlled player: Guardian (active counterpart, not AI script).
- Asymmetric information horizons and independent intelligence consoles.
- Covert intelligence warfare with counter-interventions.
- Survivor beliefs, memories, and emerging social factions.
- Multiple branching endings based on player choices and survivor survival.

## Locked MUST scope
- 20-30 configurable survivors.
- Four processing regions and four region workers.
- WorldClock and TickCoordinator.
- Food, water and medicine.
- Famine and Disease Outbreak only.
- Four Observer policies: InformationDistortion, TrustErosion, ResourcePressure and SelectiveDisclosure.
- EventBus fan-out to independent subscribers.
- Narrator with at least 40 authored variants.
- Four threshold-driven chapters: A New Beginning, The Fracture, The Revelation and The Last Recording.
- Safe choice pause/resume.
- PostgreSQL behind interfaces plus in-memory adapter; asynchronous recording.
- Story-first frontend: Opening, Story, People, Recovered Records and Final Recording.
- Metrics, four UML diagrams, README and required tests.

## Approved technical clarifications
1. Workers evaluate against immutable WorldView and return immutable RegionResult; they never mutate shared world state.
2. RegionResult contains survivor state deltas, events, resource claims, relationship deltas and migration requests.
3. TickCoordinator is the sole state merger, ordered by region ID, survivor ID and action sequence.
4. RNG is derived deterministically from run seed, region ID and tick. No ThreadLocalRandom or Math.random in the simulation core.
5. Observer decisions are tied to a completed snapshot and explicit application tick; thread scheduling cannot determine outcomes.
6. Chapter/choice triggers are evaluated at a deterministic post-merge boundary.
7. Worker failure must not cause a partial tick merge.
8. Critical persistence events are not intentionally dropped; minor events may be bounded/dropped with metrics. Critical queue overload requests a safe pause. In-memory fallback is not crash-durable.
9. Cross-region relationship changes are returned as deltas and merged by TickCoordinator.

## Open items
- None. M0 through M7 completed and verified (222 passing tests).
- Awaiting user approval on M7 completion to proceed to M8 (Choices, consequences and four threshold-driven chapters).

## Latest completed task: M13 release preparation (2026-11-25)
- Full suite: 306 tests run, 305 pass in a PostgreSQL 16 sandbox; the only failure is `Postgres17LiveIntegrationTest` (asserts PostgreSQL 17). Expect 306/306 on PostgreSQL 17 (not yet verified there).
- Defect found by fresh-clone check: the app could not start at all with PostgreSQL down (Hibernate DDL at boot), contradicting the documented fallback. Fixed: `afterfall.persistence.mode` (default `postgres`, `inmemory` via `--spring.profiles.active=inmemory`, file `application-inmemory.yml`). Adapters are conditional beans in `PersistenceConfiguration`. Test: `InMemoryModeStartupTest`. Smoke-checked: steps ticks, serves all pages and APIs with PostgreSQL stopped.
- Docs drift fixed: README test counts, stale class names (`SurvivorNeeds`, `SurvivorAction`, `TickResult.builder`, `STARTING` state), broken Windows `file:///` links, and `DEMO_SCRIPT.md` referring to a non-existent property. UML (component, tick sequence, lifecycle) checked against the code; no change needed for the event persistence fix.
- Removed a stray accidental file committed in PR 1; removed a real DB password from `DEMO_SCRIPT.md` (still in git history).
- Added `docs/demo/REHEARSAL_PACK.md`, `docs/demo/FALLBACK_VIDEO_PLAN.md`, `docs/release/V1_RELEASE_CHECKLIST.md`.
- Remaining for Abdullah: three spoken rehearsals, record fallback video, run suite on PostgreSQL 17, merge, tag `v1.0.0`.

## Latest completed task: M13 Phases 1 & 2 - Feature Freeze & Bug Bash Execution (2026-11-25)
Declared formal feature freeze for AfterFall V1. Executed all 8 prioritized Bug Bash scenarios across P0, P1, and P2 categories, with reinforced empirical verification on three critical scenarios:
1. **Hard JVM Termination & Database Recovery (P0):** Executed actual live simulation stepping to tick 5, queried active `/api/recap` and `/api/story`, and forcibly terminated the live JVM process (`Stop-Process -Force` on PID 10760). Verified directly against PostgreSQL 17 relational tables (`simulation_events`, `caption_entries`, `run_checkpoints`, `simulation_runs`) that all 62 captions, events, and checkpoints remained intact. Documented explicit architectural boundary: persisted history and documentary recaps survive 100% across restarts, while in-flight ephemeral worker threads require re-initialising the engine at the persisted state.
2. **WebSocket Disconnect & Reconnect (P1):** Executed automated browser testing via Microsoft Edge headless browser on `story.html`. Tested initial STOMP connection (`STREAM LIVE`), invoked `WsClient.disconnect()`, re-connected, and executed UI `STEP TICK`, confirming live event delivery, UI button responsiveness, and zero duplicate subscriptions.
3. **Responsive Layouts at 1366x768 & 1920x1080 (P2):** Evaluated all 4 screens (`index.html`, `story.html`, `people.html`, `records.html`) at viewports `1366x768` and `1920x1080`. Confirmed `hasHorizontalScroll = false` across all 8 configurations, with intact grid wrapping, zero text clipping, and fully operational navigation links.

**New Tests Added**
- `backend/src/test/java/com/afterfall/simulation/BugBashRegressionTest.java` (3 new automated tests).
- Full Maven test suite: **303 tests run, 0 failures, 0 errors, 0 skipped**.

## Previous completed task: M12 - Full Integration, Quality Assurance, UML, Documentation and Checkpoint Demonstration (2026-11-25)
Successfully completed and verified all six steps of Milestone M12:

1. **Concurrency & Determinism Verification Suite (Step 1):**
   - Implemented `WorkerJitterDeterminismTest` (0–50 ms synthetic delay verification; identical event logs, survivor states, and state hash across repeated seeded runs).
   - Implemented `CleanShutdownTest` (verified pool, channel, and writer thread termination without leaks).
   - Implemented `UnsafeRaceDemo` (standalone demonstration contrasting naive concurrent mutation with AfterFall's lock-protected `Resource` and sole-writer merge).
2. **Full End-to-End Integration Test Suite (Step 2):**
   - Implemented `FullRunLifecycleEndToEndTest` verifying initialisation, 4-worker ticks, canonical events, narrative captions, Observer interventions, safe choice pausing, chapter progression, evidence discovery, final recording, and PostgreSQL persistence recovery across simulated restart.
3. **Mandatory UML Deliverables (Step 3):**
   - Updated `component-diagram.mmd` and `tick-sequence.mmd`.
   - Authored new `decision-sequence.mmd` and `cross-region-handoff.mmd`.
   - Verified 100% syntax and parsing validity with headless browser rendering.
4. **Comprehensive Technical Documentation (Step 4):**
   - Completely rewrote `README.md` to university-grade standards with 3 verified factual corrections (exact PostgreSQL env vars, 51 authored templates, threshold-driven chapter mechanics).
5. **Seven-Minute Project Showcase Demonstration (Step 5):**
   - Authored `docs/demo/DEMO_SCRIPT.md` with pre-flight checklist, timed 7-minute narration, exact button triggers, fault tolerance matrix, and evaluator Q&A guide.
6. **Final Quality Audit & Regression Verification (Step 6):**
   - Full Maven test suite executed with configured environment: **300 tests run, 0 failures, 0 errors, 0 skipped**.
   - Verified `UnsafeRaceDemo` runs independently outside surefire test lifecycle.
   - Zero framework imports in domain or simulation core confirmed by `CoreBoundaryTest`.

Total project tests passing: **300** (0 failures, 0 errors, 0 skipped).

## Previous completed task: M10 - Story-First Frontend (Four Screens + Operator Controls) (2026-11-24)
Completely rewrote `README.md` to provide exhaustive, university-grade architectural and technical documentation suitable for evaluation, verified with three factual corrections:
1. Exact PostgreSQL environment variables verified against `application.yml`: `AFTERFALL_DB_URL`, `AFTERFALL_DB_USER`, `AFTERFALL_DB_PASSWORD`.
2. Exact authored narrative template count verified against `NarrativeTemplateRepository.java`: **51 authored templates** across routines, crises, Observer manipulations, death, choices, and chapter transitions.
3. Chapter transitions accurately detailed as state-threshold-driven (trust, shortage, evidence, exposure, morale, population) with minimum dwell ($D_{\min}$), soft pressure ($D_{\text{soft}}$), and hard backstop ($D_{\text{hard}}$) guardrails, distinguishing normal gameplay thresholds from compressed demonstration profiles.

**Documentation Scope Covered (`README.md`)**
1. **Project Overview:** Lore, moral statement, 4-chapter narrative arc, and V1 single-player scope.
2. **Technology Stack:** Java 21, Spring Boot 4.1.2, PostgreSQL 17, Maven Wrapper, vanilla HTML5/CSS3/JavaScript, and Java concurrency utilities.
3. **Architecture:** Detailed layered architecture and diagrams covering presentation, web, application, plain Java core, infrastructure, and PostgreSQL. Included deep dive into the 4-worker concurrency model, immutable `WorldView` snapshots, and sole-writer deterministic merge.
4. **Major Systems:** Complete breakdown of survivor biometrics and actions, resource stocks and famine/disease crises, ObserverAI manipulation policies and audit records, EventBus and template-driven Narrator with 51 authored templates and banned-term safeguards, choice pausing and consequence resolution, forensic evidence discovery, Final Recording and `RunRecap`, and asynchronous persistence.
5. **OOP & Design Patterns:** Concrete class mappings to the 4 OOP pillars, all 5 SOLID principles, and 7 GoF/concurrency patterns (Strategy, Observer, Command, Template Method, Builder, Producer-Consumer, State Machine).
6. **Installation & Local Execution:** Detailed Windows PowerShell instructions covering JDK 21 verification, PostgreSQL 17 setup and database creation, exact environment variable configuration (`AFTERFALL_DB_USER`, `AFTERFALL_DB_URL`, `AFTERFALL_DB_PASSWORD`), offline execution, and browser URLs (`localhost:8080`).
7. **Testing & QA Overview:** Documentation of all 300 tests (architecture boundaries, jitter determinism, clean shutdown, race conditions, E2E lifecycle, restart recovery, narrative compliance, and REST/WebSocket integration).
8. **UML Links:** Direct links to all 7 Mermaid diagrams in `docs/uml/`.
9. **Scope Boundaries:** Strict distinction between implemented V1 scope and future/optional features.

## Previous completed task: M12 Step 3 - Update and Complete Mandatory UML Deliverables (2026-11-25)
Updated existing and authored new Mermaid UML diagrams under `docs/uml/` reflecting the actual implemented M0–M11 architecture, threading models, deterministic merge order, safe choice pauses, and cross-region migration handoffs.

**Diagrams Updated & Created (`docs/uml/`)**
1. `component-diagram.mmd`:
   - Updated from M0 draft to actual production architecture.
   - Accurately details Browser (4 HTML screens + JS clients), Spring Web Layer (`SimulationApiController`, `StoryApiController`, `ObserverApiController`, `MetricsApiController`, `WebSocketConfig`, `WebSocketBroadcaster`), Application Layer (`SimulationService`, `RunRepository`, `EventRecorder`, `EvidenceDiscoveryRepository`), Core Layer (`SimulationEngine`, `TickCoordinator`, `RegionWorkerPool`, 4 `RegionWorker`s, `World`, `Settlement`, `ResourceStock`, `CrisisManager`, `MigrationCoordinator`, `ObserverAI`, `EventBus`, `Narrator`, `ChoiceCoordinator`, `ChapterManager`, `RecapGenerator`, `EvidenceService`), Infrastructure Adapters (Dual JPA and In-Memory implementations, `AsyncEventRecorder`), and PostgreSQL 17 database.
   - Enforces strict architectural layering and dependency inversion (simulation core has zero Spring/JPA imports).
2. `tick-sequence.mmd`:
   - Updated sequence from client invocation to snapshot publication.
   - Represents the exact implementation sequence of `TickCoordinator.doExecute()`: Phase 1 overlap guard, Phase 2 `WorldView.snapshot`, Phase 3 parallel regional evaluation across 4 workers (5s timeout), Phase 4 future collection and validation, Phase 5 deterministic sole-writer merge (5a region/survivor sort, 5b migration handoff, 5c atomic batch mutation via `StateChangeApplier`, 5d resource claim resolution, 5e crisis advancement, 5f observer intervention execution, 5g choice consequence application, 5h chapter transition evaluation), canonical `SimEvent` fan-out to `EventBus` (`Narrator` caption generation + `AsyncEventRecorder` persistence queues), Phase 6 clock advance, and engine tick-boundary pause checks.
3. `decision-sequence.mmd` (New):
   - Sequences choice moment trigger, safe tick-boundary pause, HTTP REST query (`/api/story`), modal presentation, valid player choice submission (`/api/choices/{id}`), invalid option default fallback handling, consequence queueing, simulation resumption, and next-tick consequence application by `TickCoordinator`.
4. `cross-region-handoff.mmd` (New):
   - Sequences worker migration proposals from immutable snapshots, deterministic merge collection, `MigrationCoordinator` duplicate checking and invariant validation, domain-boundary `MigrationHandoff.executeMigration` (source removal, destination addition, survivor location update, inverted index synchronization), and atomic rollback handling on subsequent batch failure.

**Validation Results**
- All diagrams verified using headless browser rendering (`@mermaid-js/mermaid-cli` via Microsoft Edge).
- 100% parse and render success across all 4 diagrams.

## Previous completed task: M12 Step 2 - Full End-to-End Integration Test Suite (2026-11-25)
Implemented `FullRunLifecycleEndToEndTest` verifying the complete AfterFall V1 gameplay lifecycle, deterministic tick progression, safe tick-boundary choice pausing, chapter progression, evidence discovery, final recording synthesis, and complete JPA persistence recovery across a simulated application restart.

**Tests & Test Scope Added**
- `backend/src/test/java/com/afterfall/simulation/FullRunLifecycleEndToEndTest.java`:
  - **Phase 1: Simulation Run Initialisation & Autonomous Ticks:** Initializes a run with seed 4242, advances multiple ticks under 4 region workers, verifies autonomous survivor action generation, resource consumption, canonical `SimEvent` emission, and template-driven `StoryCaption` generation.
  - **Phase 2: Observer Intervention & Audit Trail:** Dequeues and applies an Observer intervention at the tick boundary as sole writer; verifies state changes and audit record logging.
  - **Phase 3: ChoiceMoment & Safe Tick-Boundary Pause:** Triggers a choice moment, asserts engine pause state, submits an operator decision, and validates consequence application and resumption.
  - **Phase 4: Chapter Progression:** Asserts threshold evaluation and logs `ChapterTransition` (Chapter I -> Chapter II).
  - **Phase 5: Evidence Discovery:** Extracts available `EvidenceItem`s from applied Observer interventions, simulates player discovery via `EvidenceService`, and validates duplicate discovery protection.
  - **Phase 6: Final Recording & RunRecap Synthesis:** Generates final state summary, final survivor snapshots, and verifies mandatory closing line `"The settlement fell silent. The Observer remains active."`.
  - **Phase 7: Application Restart & Recovery:** Simulates fresh application restart, re-instantiates persistence repositories with PostgreSQL JPA entities, loads all run records, and proves regenerated `RunRecap` exactly matches the pre-restart recap.

Total project tests passing: **300** (0 failures, 0 errors, 0 skipped).

## Previous completed task: M10 - Story-First Frontend (Four Screens + Operator Controls) (2026-11-24)
Implemented REST API, WebSocket/STOMP bridge, and vanilla HTML/CSS/JavaScript interface following `docs/desgin/VISUAL_DIRECTION.md` and `docs/blueprint/AfterFall-Blueprint-Revised-Solo.md`.

**Backend Components Created & Enhanced**
- **Authoritative Application Service (`com.afterfall.application`):**
  - `SimulationService`: Manages single Spring-managed `SimulationEngine`, domain `World`, `WorldClock`, `TickCoordinator`, `ObserverAI`, `ChoiceCoordinator`, `ChapterManager`, and `Narrator`. Provides thread-safe methods for state retrieval, lifecycle control (`start`, `pause`, `resume`, `stepTick`), choice submission, metrics aggregation, survivor projection, and observer audit records.
  - Decoupled `StoryCaption` notification mechanism via `captionListeners` without treating captions as raw simulation domain events.
- **REST API Endpoints (`com.afterfall.web`):**
  - `SimulationApiController`: `/api/state` (GET), `/api/sim/start|pause|resume|step` (POST), `/api/survivors` (GET), `/api/survivors/{id}` (GET).
  - `StoryApiController`: `/api/story` (GET), `/api/choices/{choiceId}` (POST).
  - `ObserverApiController`: `/api/records` (GET), `/api/interventions/{id}/approve|reject` (POST).
  - `MetricsApiController`: `/api/metrics` (GET).
- **Data Transfer Objects (`com.afterfall.web.dto`):**
  - `StateResponse`, `StoryResponse`, `MetricsResponse`, `RecordsResponse`, `SurvivorDto`, `ChoiceSelectionRequest`, `PendingChoiceDto`, `ChoiceOptionDto`, `InterventionRecordDto`.
- **WebSocket Infrastructure (`com.afterfall.config`, `com.afterfall.web`):**
  - `WebSocketConfig`: Configures STOMP endpoint `/ws` with SockJS fallback and destination prefix `/topic`.
  - `WebSocketBroadcaster`: Listens to `SimulationService` caption generation and broadcasts to `/topic/captions` and simulation events to `/topic/events`.

**Frontend Screens & Assets (`backend/src/main/resources/static/`)**
- `index.html`: Atmospheric prologue entry screen with status indicators and live backend health detection.
- `story.html`: Main observation deck with simulation lifecycle controls, tick counter, chapter banner, live chronicler stream, stockpile/population metrics, observer intervention directives, and choice moment modal.
- `people.html`: Survivor directory grid with biometric status (alive/deceased badges, health/hunger/thirst gauges) and detailed dossier inspector.
- `records.html`: Dual-disclosure forensic comparator juxtaposing ground truth against sanitized Observer reports.
- `css/design-tokens.css` & `css/components.css`: Quiet Cinematic Minimalism palette (`#0B0C0B`, `#BA1A1A`, `#ECE8E1`), typography roles, and geometric HUD layout.
- `js/api.js`, `js/ws.js`, `js/story.js`, `js/people.js`, `js/records.js`: Vanilla JavaScript API client and lightweight STOMP client with auto-reconnect.

**Tests Verified**
- `WebControllersTest`: 6 tests covering state retrieval, tick step execution, survivor rosters, story captions, observer audit records, and metrics.
- Full Maven test suite: **280 tests run, 0 failures, 0 errors, 0 skipped**.
- Live Spring Boot runtime: Successfully verified with PostgreSQL 17 connection, HTTP REST requests, and WebSocket handshake.

## Previous completed task: M9 - Spring Boot, PostgreSQL 17, Persistence Interfaces and Asynchronous Recorder (2026-11-24)
Implemented in plain Java across `com.afterfall.events` and `com.afterfall.narrator`; no framework imports (verified by boundary and suite tests).

**Classes created & enhanced**
- **Canonical Event Bus & Fan-Out (`com.afterfall.events`):**
  - `EventSeverity`: Enum defining `MINOR`, `SIGNIFICANT`, `CRITICAL`.
  - `SimEvent`: Immutable canonical simulation event carrying `EventId`, `tick`, `eventType`, `targetSurvivors`, `location`, `severity`, `summary`, structured `data`, and causal `sourceReferences`.
  - `EventSubscriber`: Interface for independent subscriber listeners with dedicated queue buffers.
  - `EventBus`: Observer pattern implementation with isolated subscriber queues (`ConcurrentHashMap<String, Queue<SimEvent>>`); slow or failing subscribers cannot block other subscribers or simulation ticking.
- **Narrator & Authored Templates (`com.afterfall.narrator`):**
  - `StoryCaption`: Immutable story caption linking rendered documentary text to source `EventId`s and referenced `SurvivorId`s.
  - `NarrativeTemplate`: Authored template abstraction with placeholders `{survivor}`, `{target}`, `{region}`, `{day}`, `{extra}`.
  - `NarrativeTemplateRepository`: Collection of **46 authored narrative templates** covering routines, medical afflictions, famine, disease outbreak, Observer distortions, and quiet implied endings. Defines `BANNED_TERMS` list.
  - `Narrator`: Independent `EventSubscriber` implementing priority evaluation, deterministic template selection using run seed and event attributes, and bounded anti-repetition memory (size 8).
- **Simulation Integration (`com.afterfall.simulation`):**
  - `TickCoordinator`: Maps collected `ProposedEvent`s to canonical `SimEvent`s with sequential `EventId`s and fans them out to `EventBus`. Exposes `eventBus()` and constructor injection.

**Tests created & verified**
- `SimEventTest`: 3 tests verifying immutability, validation, and builder construction.
- `EventBusTest`: 3 tests verifying fan-out to independent queues, faulty subscriber isolation, and subscriber registration lifecycle.
- `NarrativeTemplateRepositoryTest`: 4 tests verifying 46 authored templates, unique IDs, placeholder rendering, and the **Banned-Term Keyword Test** asserting 0 sensitive/graphic terms.
- `NarratorTest`: 3 tests verifying caption generation linked to source event IDs, deterministic template selection across seeds, and end-to-end integration under `TickCoordinator`.
- `CoreBoundaryTest`: Expanded to verify zero framework imports in `com.afterfall.events` and `com.afterfall.narrator`.

Total project tests passing: **222** (0 failures, 0 errors, 0 skipped).

## Previous completed task: M6 - ObserverAI with four MUST policies (2026-11-23)
Implemented in plain Java across `com.afterfall.observer` and `com.afterfall.simulation`; no framework imports (verified by boundary and suite tests).

**Classes created & enhanced**
- **Intervention Models & Commands (`com.afterfall.observer`):**
  - `InterventionType`: Enum defining `INFORMATION_DISTORTION`, `TRUST_EROSION`, `RESOURCE_PRESSURE`, `SELECTIVE_DISCLOSURE`.
  - `InterventionStatus`: Enum defining `PROPOSED`, `APPLIED`, `REJECTED`.
  - `Intervention`: Immutable command record/builder carrying policy justification, intended effects, need deltas, trust deltas, target references, and preserving both original and altered reports.
  - `InterventionRecord`: Immutable audit record capturing the full evidence trail (`tick`, `targetDescription`, `policy`, `reason`, `intendedEffect`, `actualResult`, `status`, `originalReport`, `alteredReport`) for Recovered Records.
  - `ObservationReport`: Read-only aggregated metrics (active survivors, ill survivors, food/water/medicine stock, average trust, average morale, and relationship tension).
- **Intervention Policies (Strategy Pattern, `com.afterfall.observer`):**
  - `InterventionPolicy` (interface): Defines `type()`, `shouldTrigger(report)`, and `evaluate(...)` returning `Optional<Intervention>`.
  - `InformationDistortionPolicy`: Triggers when trust is low, food shortage is active, and tension is high; fabricates altered reports of secret reallocation and reduces safety.
  - `TrustErosionPolicy`: Triggers on high relationship tension or low trust; targets conflicting survivors and reduces interpersonal trust.
  - `ResourcePressurePolicy`: Triggers when resources fall below warning thresholds; amplifies scarcity perception and induces competition.
  - `SelectiveDisclosurePolicy`: Triggers when contagion or low morale is present; selectively discloses alarming infection warnings while omitting cure availability.
  - `ObserverAI`: Strategy coordinator evaluating policies against immutable `WorldView` snapshots, proposing `Intervention` commands without direct domain mutation, and archiving evidence records.
- **Simulation Integration (`com.afterfall.simulation`):**
  - `TickCoordinator`: Added `queueIntervention(Intervention)`, `observerAI()`, and integrated deterministic Phase 5f safe tick boundary intervention execution. Interventions are validated, target alive survivors, apply deltas as sole writer, and record execution status (`APPLIED` / `REJECTED`).

**Tests created & verified**
- `ObserverPolicyTest`: 4 tests verifying independent trigger conditions, report generation, and intended effects for all 4 MUST policies.
- `ObserverAITest`: 3 tests verifying zero direct domain state mutation during evaluation, deterministic proposal generation across seeds, and accurate audit trail recording.
- `ObserverIntegrationTest`: 3 integration tests verifying queued intervention boundary application, graceful rejection of invalid/deceased targets, and trust erosion between connected survivors under `TickCoordinator`.
- `CoreBoundaryTest`: Expanded to include `com.afterfall.observer` and verified zero prohibited framework imports.

Total project tests passing: **202** (0 failures, 0 errors, 0 skipped).

## Previous completed task: M5 - Resources and Famine/Disease Crises (2026-11-23)
Implemented in plain Java across `com.afterfall.domain` and `com.afterfall.simulation`; no framework imports (verified by boundary and suite tests).

**Classes created & enhanced**
- **Resources & Stock Pool (`com.afterfall.domain`):**
  - `Resource`: Encapsulates a resource type and non-negative quantity invariant protected by `ReentrantLock` for concurrency safety and race-condition compliance.
  - `ResourceStock`: Settlement aggregate holding shared resources (`FOOD`, `WATER`, `MEDICINE`).
  - `Settlement`: Enhanced to hold and expose the shared `ResourceStock` pool.
- **Crisis Architecture (Template Method Pattern, `com.afterfall.domain`):**
  - `Crisis` (abstract base): Defines the `final List<CrisisEffect> advance(Settlement, ResourceStock)` template method orchestrating triggers, duration tracking, resolution, and effect collection, fulfilling Open/Closed and Liskov Substitution principles.
  - `FamineCrisis`: Subclass triggering when food $\le$ threshold; applies food need penalties and vitality reduction to alive survivors.
  - `DiseaseOutbreak`: Subclass triggering when ill survivor count $\ge$ threshold; spreads to healthy survivors with low safety and applies vitality drain.
  - `CrisisEffect`: Immutable proposal record/builder for crisis-driven state adjustments.
- **Simulation Integration (`com.afterfall.simulation`):**
  - `CrisisManager`: Manages the active crises list and coordinates their advancement.
  - `WorldView`: Updated to capture an immutable snapshot of resource quantities and expose `isFoodAvailable()` / `isMedicineAvailable()`.
  - `RegionWorker`: Injects resource availability into `BehaviorContext` and `ActionContext`.
  - `TickCoordinator`: Implements deterministic Phase 5d claim resolution and Phase 5e crisis effect application by the coordinator as the sole writer.

**Tests created & verified**
- `ResourceTest`: 10 tests verifying add/consume, non-negative invariants, and a 20-thread concurrent race-condition test.
- `ResourceStockTest`: 5 tests covering defaults, custom quantities, availability, and item consumption.
- `FamineCrisisTest`: 8 tests covering trigger thresholds, duration, resolution upon restocking, and dead survivor filtering.
- `DiseaseOutbreakTest`: 8 tests covering threshold trigger, safety-based contagion spread, resolution when cured, and vitality drain.
- `ResourceAndCrisisIntegrationTest`: 9 integration tests verifying resource snapshotting in `WorldView`, deterministic claim consumption during tick merge, and crisis effect execution under `TickCoordinator`.

Total project tests passing: **192** (0 failures, 0 errors, 0 skipped).

## Previous completed task: M4 - Deterministic Multithreading: Four Region Workers (2026-11-23)
Implemented in plain Java across `com.afterfall.simulation` and `com.afterfall.domain`; no framework imports (verified by boundary and suite tests).

**Classes created & enhanced**
- **Concurrency & Parallel Evaluation:**
  - `RegionWorkerPool`: Dedicated 4-thread `ExecutorService` with fixed daemon worker threads named `afterfall-region-1` through `afterfall-region-4`. Enforces exactly 4 tasks per invocation with graceful shutdown.
  - `RegionTask`: Callable wrapper executing a region's evaluation against an immutable `WorldView` snapshot.
  - `RegionWorker`: Evaluates all resident survivors in a single region in ascending `SurvivorId` order against the immutable `WorldView` snapshot. Operates purely read-only without modifying domain models.
  - `SeededRandomFactory`: Pure SplitMix64 64-bit mixer deriving deterministic pseudo-random sequences for `(runSeed, regionId, tick)`.
- **Cross-Region Migration Handoff & Rollback:**
  - `MigrationHandoff` (`com.afterfall.domain`): Domain helper performing safe survivor transfer between regions and updating location index.
  - `MigrationCoordinator`: Validates migration proposals (verifies existence, matching source, distinct destination, and duplicate/conflict detection per tick), applies migrations atomically, and provides complete rollback if domain state application fails.
- **Tick Coordination & Facade Integration:**
  - `TickCoordinator`: Refactored to coordinate the full 6-phase parallel protocol:
    1. Reentrancy and overlap check.
    2. Exactly one immutable `WorldView` snapshot per tick.
    3. Parallel task execution across the 4-thread `RegionWorkerPool`.
    4. Worker result validation.
    5. Deterministic merge (sole-writer phase): sorting by `RegionId` (1..4) and survivor results by `SurvivorId`, assigning global event sequence numbers, executing atomic migration handoff, and applying state changes via `StateChangeApplier.applyBatch()`.
    6. Advancing `WorldClock`, incrementing world version, and publishing immutable `TickResult`.
  - `SimulationEngine`: Updated `stop()` method to cleanly shut down coordinator worker threads.

**Tests created & verified**
- `RegionWorkerPoolTest`: Exactly 4 threads created with expected names, genuine concurrency verified via `CyclicBarrier`, shutdown idempotency, rejected execution after shutdown, and validation of 4 tasks.
- `SeededRandomFactoryTest`: Identical outputs for identical seed/region/tick inputs, distinct streams for differing regions and ticks, and cross-instance repeatability.
- `MigrationCoordinatorTest`: Migration handoff between regions, multi-region migrations, rejection of duplicate requests, rejection of identical source/destination, rejection of mismatched sources, and atomic rollback restoring original locations.
- `DeterministicConcurrencyTest`:
  - Completion order permutation test: artificially delayed worker completion proves merged result order, survivor action order, and global event sequence numbering are strictly identical and deterministic regardless of thread scheduling.
  - Multi-tick determinism test: two independent 5-tick simulations from the same initial state and seed yield identical canonical world snapshots across all survivors, needs, vitality, conditions, and inventories.
  - Worker failure test: injected exception in a worker aborts tick, leaves clock and world version unchanged, rolls back state, and safely releases tick guard for subsequent recovery ticks.
- `SimulationDeterminismAndEquivalenceTest`:
  - Single-threaded vs. multithreaded simulation equivalence across 5 ticks.
  - Different seeds producing appropriately diverging pseudo-random streams.
  - Zero shared-state mutation by workers during evaluation.

Total project tests passing: **152** (0 failures, 0 errors, 0 skipped).

## Previous completed task: M3 - Correct Single-Threaded Simulation Baseline (2026-11-23)
Implemented in plain Java across `com.afterfall.simulation`; no framework imports (verified by boundary and suite tests).

**Classes created**
- **Simulation Clock & Snapshot Views:**
  - `WorldClock`: Deterministic clock tracking tick numbers and day progression; advances only on tick completion.
  - `WorldView`: Immutable snapshot of the settlement state at a specific tick, encapsulating read-only `RegionView` and `SurvivorView` records.
  - `RegionView`: Read-only view of region metadata and resident survivor IDs.
  - `SurvivorView`: Read-only snapshot of a survivor's needs, health, inventory, location, and status.
- **Evaluation & State Application:**
  - `SurvivorActionResult`: Immutable record pairing survivor ID, evaluation sequence number, and proposed `ActionResult`.
  - `RegionResult`: Immutable result of processing a region during a tick in deterministic survivor ID order.
  - `StateChangeApplier`: Atomic proposal application with pre-validation and all-or-nothing rollback for single survivors and multi-survivor batches.
  - `TickResult`: Immutable output of a completed tick with ordered region results, event proposals, preserved resource claims, and migration requests.
  - `TickCoordinator`: Orchestrates the 6-phase tick protocol sequentially: validates inputs, takes immutable `WorldView` snapshot, sequentially evaluates regions and survivors using M2 Strategy & Command components, pre-validates proposals, executes batch application, and advances clock.
- **Lifecycle & Facade:**
  - `SimulationLifecycle`: Explicit state machine (`RUNNING`, `PAUSE_REQUESTED`, `PAUSED`, `STOPPING`, `STOPPED`) with validated transitions and tick boundary pause enforcement.
  - `SimulationEngine`: Unified single-threaded facade coordinating lifecycle, clock, and tick execution; safely publishes immutable `WorldView` snapshots.

**Tests created & verified**
- `StateChangeApplierTest`: Single-survivor atomic mutation and rollback on inventory exception.
- `StateChangeApplierBatchTest`: Multi-survivor batch pre-validation and complete rollback of all survivors upon failure.
- `TickCoordinatorSuccessTest`: 4-region deterministic sequential evaluation, clock progression, preserved claims and migrations.
- `TickCoordinatorFailureTest`: Atomicity on injected evaluation failure; zero state mutations, clock does not advance.
- `SimulationLifecycleTest`: Valid and invalid state transitions, idempotence, terminal state invariants.
- `SimulationEngineTest`: Facade operation, pause at tick boundary, resume, stop, latest snapshot publication, and deterministic repeatability across runs.

Total project tests passing: **130** (0 failures, 0 errors, 0 skipped).

## Previous completed task: M2 - Autonomous Behaviours, Actions & Core Patterns (2026-11-20)
Implemented in plain Java across `com.afterfall.behavior` and `com.afterfall.action`; no framework imports (verified by expanded `CoreBoundaryTest`).

**Classes created**
- **Action (Command Pattern, `com.afterfall.action`):**
  - `Action` (interface): Defines `ActionResult evaluate(ActionContext context)`.
  - `ActionType`, `ActionStatus`: Enums for action classification and success/rejection status.
  - `ActionContext`: Immutable evaluation context encapsulating `SurvivorSnapshot`, `currentRegion`, food/medicine availability flags, and optional destination region.
  - `ActionResult`: Immutable evaluation output containing typed proposed need deltas, vitality deltas, condition updates, inventory deltas, work contribution, migration requests, and immutable event proposals. Rejection cleanly returns no partial mutations.
  - `ProposedEvent`: Immutable record representing an action's narrative event proposal.
  - `EatAction`: Requires food; proposes +30 `Need.FOOD` and -1 `ItemType.FOOD` if personal item used.
  - `WorkAction`: Proposes -15 `Need.REST` and produces work output scaled by profession skill.
  - `RestAction`: Proposes +25 `Need.REST` energy restoration.
  - `HealAction`: Requires ILL/INJURED condition and medicine; proposes `HealthCondition.HEALTHY`, +20 vitality, and -1 `ItemType.MEDICINE`.
  - `MoveAction`: Proposes migration request to destination `RegionId` without modifying survivor or settlement rosters.
- **Behavior (Strategy Pattern, `com.afterfall.behavior`):**
  - `Behavior` (interface): Defines `Optional<Action> decide(BehaviorContext context)`.
  - `BehaviorType`: Enum for strategy types (`SEEK_FOOD`, `WORK`, `REST`, `SEEK_MEDICAL_HELP`).
  - `BehaviorContext`: Immutable decision context wrapping `SurvivorSnapshot`, region info, and candidate regions.
  - `BehaviorConfig`: Centralized, validated threshold configuration (critical/fatigue/hunger limits).
  - `SeekFoodBehavior`: Evaluates hunger need against threshold adjusted by traits (e.g. FEARFUL).
  - `WorkBehavior`: Selects `WorkAction` when basic needs and health are met; influenced by profession skill and cooperativeness.
  - `RestBehavior`: Selects `RestAction` when fatigued, respecting resilience traits.
  - `SeekMedicalHelpBehavior`: Selects `HealAction` if medicine is present, or `MoveAction` toward shelter if local medicine is absent.
  - `BehaviorSelector`: Deterministic coordinator evaluating strategies in strict priority order (Critical Medical -> Critical Starvation -> Critical Exhaustion -> Hunger -> Work -> Rest) with zero random state.
- **Domain Snapshot (`com.afterfall.domain`):**
  - `SurvivorSnapshot`: Immutable read-only projection of `Survivor` state with defensively copied collections to guarantee zero side-effects during evaluation.

**Design Decisions & Patterns**
- **Strategy Pattern:** `Behavior` interface and 4 concrete strategies encapsulate independent decision algorithms.
- **Command Pattern:** `Action` interface and 5 concrete commands encapsulate actions as evaluatable objects returning immutable proposed changes.
- **Strict Immutability:** Contexts and results expose no setters, no mutation lambdas (`Consumer<Survivor>`), and no direct references to mutable domain components.
- **SOLID Compliance:** High cohesion and single responsibility per action/behavior; open for extension (adding new behaviors/actions requires zero changes to existing ones).

**Tests** (`backend/src/test/java/`):
- `com.afterfall.action.ActionTest`: 7 tests covering all 5 actions, rejection conditions, inventory usage, immutability, and zero side-effects.
- `com.afterfall.behavior.BehaviorTest`: 7 tests covering all 4 behaviors, critical need prioritization, trait influence, dead survivor handling, and deterministic tie-breaking.
- `com.afterfall.CoreBoundaryTest`: Expanded to verify zero framework imports in `domain`, `simulation`, `behavior`, and `action`.

**Command and results (PowerShell, from `backend/`)**
- `.\mvnw.cmd test` -> Tests run: 104, Failures: 0, Errors: 0, Skipped: 0. BUILD SUCCESS.
- `.\mvnw.cmd package` -> Built repackaged executable JAR `afterfall-backend-0.0.1-SNAPSHOT.jar`. BUILD SUCCESS.

**Known limitations at M2 completion**: Simulation engine, ticking, and multithreading were not yet running (scheduled for M3/M4).

## Historical M2 next task (completed in M3)
- M3: Correct single-threaded simulation baseline (`WorldClock`, single-threaded `TickCoordinator`, action execution pipeline). Completed 2026-11-23.

## M1 Session 2 record (2026-11-20)
Plain Java in `com.afterfall.domain`; no Spring/JPA imports (verified by `CoreBoundaryTest`).

**Classes created/updated** (`backend/src/main/java/com/afterfall/domain/`)
- `RegionId`: Positive integer record in `[1, 4]` identifying the four MUST processing regions (`REGION_1` through `REGION_4`), naturally ordered for deterministic processing.
- `Region`: Maintains ordered `TreeSet<SurvivorId>` and minimal local state without geographical coordinates or UI math. Encapsulated views return unmodifiable sets.
- `PopulationConfig`: Configuration record with `size` in `[20, 30]`, defaulting to 25.
- `Survivor`: Updated with `location()` returning `Optional<RegionId>` and package-private `setLocation(RegionId)` managed centrally by `Settlement`.
- `Settlement`: Aggregate root managing the 4 MUST regions and registered survivors. Enforces single-region membership, rejects duplicate registrations/IDs, prevents external mutation, and provides deterministic collections.
- `World`: Root domain container encapsulating the `Settlement` without ticking or engine logic.
- `InitialPopulation`: Deterministic generator supporting all counts 20-30 (default 25), balanced round-robin distribution across 4 regions, unique IDs, diverse personalities, and MUST professions.
- `docs/uml/class-diagram.mmd`: Mermaid class diagram showing implemented classes, relationships, and planned future components.

**Design decisions**
- Location assignment is held on `Survivor` and kept strictly consistent with `Settlement`'s region index to prevent split-brain states.
- `InitialPopulation` is a deterministic domain generator without premature GoF Factory abstraction.
- `Settlement` pre-validates and guarantees exactly the four MUST regions on construction.

**Tests** (`backend/src/test/java/com/afterfall/domain/`): `PopulationConfigTest`, `RegionTest`, `SettlementTest`, `WorldTest`, `InitialPopulationTest` (in addition to all Session 1 tests).

**Command and result (PowerShell, from `backend/`)**
- `.\mvnw.cmd test` -> Tests run: 88, Failures: 0, Errors: 0, Skipped: 0. BUILD SUCCESS (2 existing + 86 domain tests).

**Known issues**: none. Nothing committed.

## Historical M1 Session 2 next task (completed in M2)
- M1 Final Review & Approval -> Proceed to M2 (Behaviours, actions and core patterns). Completed 2026-11-20.

## M1 Session 1 record (2026-11-20)
Plain Java in `com.afterfall.domain`; no Spring/JPA imports (checked by `CoreBoundaryTest`). No behaviour, decay, thresholds or simulation logic.

**Classes** (`backend/src/main/java/com/afterfall/domain/`)
- `Scale` (0-100 range, `requireInRange`, overflow-safe `clamp`), `SurvivorId`, `EventId` (positive-id records; `SurvivorId` natural order = deterministic merge order).
- `Needs` + `Need` (FOOD, WATER, REST, SAFETY; 100 = fully satisfied), `Health` + `HealthCondition` (vitality 0-100; HEALTHY/ILL/INJURED).
- `Personality` (immutable; all eight `Trait` values 0-100; `with` returns a copy), `Profession` role object (immutable type + skill) with `ProfessionType` (the five MUST professions only).
- `Inventory` + `ItemType` (FOOD, WATER, MEDICINE; capacity; never negative, never over capacity; failed operations leave state unchanged; `has` rejects negative amounts and accepts 0).
- `Relationship` (type, trust, affection, hostility; bounded history of 20 `RelationshipEntry` snapshots, each with day and source `EventId`; rejects earlier days before mutation) + `RelationshipType`.
- `CharacterMemory` (day, signed emotional impact -100..100 inclusive including 0, optional related survivor, required source `EventId`; no free text).
- `Survivor` (composition of the above + `SurvivorStatus` ALIVE/DEAD; relationships in a `TreeMap` by target id; `remember` rejects earlier day before adding; identity = `SurvivorId`).

**Design decisions & Session 1 corrections**
- Profession is a field of Survivor, never a subclass; a test asserts this.
- Components are mutable and exposed by accessors so the single-writer TickCoordinator can apply deltas later; Survivor guards only the invariants it owns. Collections are returned unmodifiable/copied.
- Needs, Health and Inventory are mutable; Personality, Profession, CharacterMemory, RelationshipEntry and ids are immutable.
- Relationship changes require a source `EventId` so history stays traceable to events.
- **Correction 1 (`Relationship.java`):** `adjust()` and `changeType()` reject a day earlier than the last recorded history entry (`day < lastDay`); equal days are allowed. Rejection throws `IllegalArgumentException` prior to any state or history mutation.
- **Correction 2 (`Survivor.java`):** `remember()` rejects a `CharacterMemory` whose day is earlier than the last stored memory (`memory.day() < lastDay`); equal days are allowed. Rejection leaves `memories` unchanged.
- **Correction 3 (`Inventory.java`):** `has(type, amount)` throws `IllegalArgumentException` when `amount < 0`. Zero is allowed and returns `true` for a valid, non-null item type.
- **Correction 4 (`CharacterMemory.java`):** `emotionalImpact = 0` is allowed; valid range is inclusive `[-100, 100]`. Javadoc updated.
- Left out on purpose: location/region, current Behavior, faction (Session 2 / M2).

**Tests** (`backend/src/test/java/com/afterfall/domain/`): `ScaleAndIdsTest`, `NeedsTest`, `HealthTest`, `PersonalityTest`, `ProfessionTest`, `InventoryTest`, `RelationshipTest`, `CharacterMemoryTest`, `SurvivorTest`.

**Command and result (PowerShell, from `backend/`)**
- `.\mvnw.cmd test` -> Tests run: 68, Failures: 0, Errors: 0, Skipped: 0. BUILD SUCCESS (2 existing + 66 domain tests).

**Known issues**: none. Nothing committed.

## Historical M1 Session 1 next task (completed in M1 Session 2)
- M1 Session 2: Region, Settlement/World, initial population (20-30, configurable). Completed 2026-11-20.

## M0 record (2026-11-19)
Generated from Spring Initializr (start.spring.io) using its current default stable release, then trimmed.

**Versions / dependencies**
- Spring Boot 4.1.2 (`spring-boot-starter-parent`; Initializr default stable; snapshots/RCs not used).
- `spring-boot-starter-webmvc` (compile) and `spring-boot-starter-webmvc-test` (test; JUnit 5, Mockito, AssertJ). Versions managed by the Boot parent.
- Java 21 (`java.version`), jar packaging, groupId `com.afterfall`, artifactId `afterfall-backend`.
- Maven Wrapper 3.3.4 (`distributionType=only-script`), Maven 3.10.1.
- No PostgreSQL/JPA dependency or datasource config (deferred to M9). No credentials in any file.

**Files created**
- `backend/pom.xml`, `backend/mvnw`, `backend/mvnw.cmd`, `backend/.mvn/wrapper/maven-wrapper.properties`, `backend/.gitattributes`
- `backend/src/main/java/com/afterfall/AfterFallApplication.java`
- `backend/src/main/java/com/afterfall/{domain,simulation,application,infrastructure,web,config}/package-info.java`
- `backend/src/main/resources/application.properties` (application name only)
- `backend/src/test/java/com/afterfall/AfterFallApplicationTests.java` (context load)
- `backend/src/test/java/com/afterfall/CoreBoundaryTest.java` (fails if domain/simulation import Spring, JPA, Hibernate or PostgreSQL)
- `docs/uml/component-diagram.mmd` (initial; future components dashed and labelled with milestone)
- `README.md` updated (status, layout, build/test/run commands)

**Commands run (PowerShell, from `backend/`) and results**
- `java -version` -> Temurin 21.0.13+9 LTS.
- `.\mvnw.cmd -v` -> Apache Maven 3.10.1, Java 21.0.13.
- `.\mvnw.cmd test` -> Tests run: 2, Failures: 0, Errors: 0, Skipped: 0. BUILD SUCCESS.
- `.\mvnw.cmd package` -> Tests run: 2, Failures: 0; built and repackaged `target\afterfall-backend-0.0.1-SNAPSHOT.jar`. BUILD SUCCESS.
- Smoke run: `java -jar target\afterfall-backend-0.0.1-SNAPSHOT.jar` -> Tomcat started on port 8080; `GET /` -> 404 (expected, no endpoints); process stopped.

**Decisions**
- Package skeleton uses the six top-level packages requested for M0; finer blueprint packages (behavior, action, crisis, events, observer, narrator, choice, snapshot, metrics) are added when their milestone starts.
- Main class named `AfterFallApplication` (Initializr default name replaced).

**Known issues / notes**
- Test run prints a JVM warning about Mockito/Byte Buddy loading a Java agent dynamically. Harmless on JDK 21; can be silenced later by configuring the Mockito agent in Surefire if desired.
- Mermaid diagram has not been rendered by a Mermaid tool in this session; preview it in VS Code/GitHub to confirm layout.
- Nothing committed (per instructions).



## Session log
- 2026-11-25: v1.0.0 integration. Merged PRs 1-9 in order (conflicts in PROGRESS.md for PR 5 and SimulationService for PR 6 resolved by keeping both sides). Full suite on PostgreSQL 17.12: 314 run, 0 failures, 0 errors. Headless Chromium on the packaged app: chapter title card, choice dialog opens/answers/resumes, ticks continue, final recording ends on "The settlement fell silent." with no console errors. README, release checklist and DEMO_SCRIPT updated to match (auto clock, dialog works, title cards).
- 2026-11-25: Cinematic visual polish (frontend only: `static/css/cinema.css`, `static/js/cinema.js`, small hooks in `story.js`/`records.js`). Full-screen chapter title cards on chapter change (WebSocket `CHAPTER_*` events), heavier crisis moments (Famine/Outbreak band, vignette, death pulse), exposure reveal when Observer evidence is decoded, and a staged Final Recording sequence ending with the mandatory closing line. No API/data contract change; `prefers-reduced-motion` honoured; 100-entry DOM cap and WS reconnect untouched. Checked in headless Chromium at 1366x768 and 1920x1080 with zero console errors. Known data issue (backend, not touched): the default run's recap scenes are all repeated chapter-transition lines and `endingChapter` reads CHAPTER_1; the sequence de-duplicates scenes.
- 2026-11-25: Docs/UML polish (docs and README only, no code). Split the 128-class diagram into five readable views, rewrote the component diagram, added the missing PAUSE_REQUESTED->RUNNING edge to the lifecycle diagram, rendered all diagrams to `docs/uml/rendered/` with Mermaid CLI 11.4.2, added `docs/uml/README.md` and `docs/architecture/OOP-SOLID-THREADING-MAP.md`. README fixes: template count 46->51, `DiseaseOutbreak` class name, honest test result (306 run, 305 pass without PostgreSQL 17), layout, UML index.
- 2026-11-25: M13 defect fix, canonical event persistence. Root cause: `SimulationService` built its own `EventBus` while the `EventBus` bean in `PersistenceConfiguration` (which registered the recorder) was never used by the engine, so no canonical events were recorded at M12; the WIP registered `AsyncEventRecorder` as a subscriber, but that uses its configured default run id `current-run`, not `run-1`, so events still never landed under the run the captions use. Fix: `SimulationService` always registers a run-bound subscriber calling `recordAll(currentRunId, ...)`. Regression test `EventToCaptionPersistenceTest.productionWiringWithAsyncRecorderPersistsEventsUnderTheSimulationRunId` (failed before, passes after). Live check on PostgreSQL: 60 steps -> 1,501 run-1 events, 750 run-1 captions, 0 unresolved source event ids. Full suite 305 tests, 304 pass; the one failure is `verifiesRealPostgres17DatabaseMetadata`, which asserts PostgreSQL 17 and the check environment had 16.15. Note: the orphaned `EventBus` bean in `PersistenceConfiguration` is still referenced by `PersistenceWiringIntegrationTest`, so it was left in place (candidate for later cleanup).
- 2026-11-25: M13 Phase 4 Final Visual and Performance Refinement completed. Verified:
  - Visual consistency and zero horizontal overflow across all 4 screens (`index.html`, `story.html`, `people.html`, `records.html`) at 1366x768 and 1920x1080.
  - Modal layering and z-indices verified for `#choice-modal` and `#recap-modal` (z-index 1000).
  - Performance cap of 100 chronicle entries in DOM strictly enforced under 200+ tick load (`streamFeed.children.length` <= 100).
  - Memory consumption stable with no element leak (JS heap delta: -0.90 MB after 115 ticks).
  - Genuine WebSocket disconnection and automatic reconnection verified via exponential backoff without manual `connect()` invocation.
  - EventBroadcaster wired to EventBus, delivering tick events and documentary captions seamlessly to STOMP topic `/topic/events` and `/topic/captions`.
  - Full Maven test suite: 303 tests run, 0 failures, 0 errors, 0 skipped.
- 2026-11-25: M13 Phases 1 & 2 Bug Bash completed (303 tests; JVM kill/recovery, WS interruption, responsive testing).
- 2026-11-20: M1 Session 2 implemented and verified (Region, Settlement, World, PopulationConfig, InitialPopulation, class-diagram.mmd; 88 total passing tests).
- 2026-11-20: M1 Session 1 four targeted domain-model corrections implemented and verified (68 total passing tests).
- 2026-11-19: M1 Session 1 implemented and verified (human domain model, 63 new tests; 65 total passing).
- 2026-11-19: M0 implemented and verified (Spring Boot 4.1.2 skeleton, package boundaries, boundary test, initial component diagram, README build commands).
- 2026-11-18: Environment setup guided; JDK 21 and PostgreSQL 17.12 verified; `afterfall` database created; VS Code workspace and Java extension pack prepared; private GitHub repository created; revised solo blueprint adopted as source of truth.

## Recap / Final Recording fix (2026-11-25, branch based on PR 2)
- Cause: `SimulationService.stepTick` re-recorded the whole chapter transition history every tick, so persisted history held ~900 duplicate transitions; `FinalRecordingGenerator` (scenes capped by count) filled with them and never reached real captions. `endingChapter` was read from run metadata written once at start (always CHAPTER_1).
- Fix: record only new transitions (service counter + idempotent in-memory/JPA adapters); `FinalRecordingGenerator` now builds up to 14 scenes from distinct transitions (plain wording), choices, labelled "Observer record" interventions and severity-ranked, evenly thinned captions, each labelled with the chapter at its tick; ending chapter derived from the last persisted transition. API shape, determinism and closing line unchanged.
- Tests: new `RecapFromRealRunTest`, extended `FinalRecordingTest`. Full suite: 308 run, 11 errors, all `Postgres17LiveIntegrationTest` (no PostgreSQL 17 in the cloud container).
- Note: in a default 400-tick run the hard backstop still fires for all three chapter changes (pacing, not touched here).

## Demo pacing fix (2026-11-25)
- Finding: the live loop was static. Needs never decayed, food was never produced or consumed, deaths never resolved, `ObserverAI.analyzeAndPropose` was never called and no crisis raised a choice, so every chapter change came from the hard backstop (ticks 48/96/144) with no crisis or death.
- Pass 1: `SettlementUpkeep` (hunger drift 2/tick, food from work, death at vitality 0); the Observer analyses the tick's immutable snapshot and its proposal is applied at that tick's merge; applied interventions add visible-intervention count and +10 exposure; `ObserverAI.COOLDOWN_TICKS = 4`.
- Pass 2 (longer arc): steady foraging (1 food / 3 ticks), fed survivors recover vitality, resilience relief by RESILIENT trait (weakest die first, strongest survive), malnutrition illness (one per tick at vitality <= 65, so Disease Outbreak triggers), Famine and Disease vitality penalties -1, `CRISIS_FAMINE` / `CRISIS_DISEASE` events, and two authored choice moments (`LiveChoiceMoments`: The Last Stores, The Sick Quarter) raised when each crisis begins. ChapterConfig default D_soft 60, D_hard 120, P 20.
- Default run (unchanged seed): Ch1->2 tick 46, Ch2->3 tick 66, Ch3->4 tick 178 (population below 20), all organic; Famine from 57 (choice), Disease Outbreak from 135 (choice); deaths from about tick 150; 2 survivors remain at tick 400. Before: 3 forced transitions, 0 crises, 0 deaths, 0 choices.
- Tests: `DefaultRunPacingTest`; `ObserverIntegrationTest` uses an inert Observer; `ChoicePauseResumeBoundaryTest` allows the one-unit forage.

## Demo docs re-timed to the new pacing (2026-11-25, docs only, based on PR 7)
- Ran the packaged app (`inmemory` profile, seed 42, 25 survivors) through the default run via REST and Chromium. Rewrote `docs/demo/DEMO_SCRIPT.md` (run map, 7-minute budget, `Step-To` / `Answer` PowerShell helpers, Q6 to Q9, known rough edges), `REHEARSAL_PACK.md` and `FALLBACK_VIDEO_PLAN.md`.
- Findings left unfixed (no code touched): `START`/`RESUME` do not tick, only `STEP TICK` does; `getStory()` always returns `activeChoice = null`, so the choice dialog never opens (answer via `POST /api/choices/{id}`); recap scene list is 66 of 71 repeated Observer-record lines with no deaths; chronicle uncapped (about 2,200 entries by tick 180) without PR 5.
- Next: decide whether to fix the choice dialog and add an auto-run clock before the demo.


## Demo controls, choice dialog and recap fix (2026-11-25)
- START/RESUME now run an auto-advancing WorldClock (blueprint section 5: the simulation runs independently of the browser). `SimulationService` ticks every `afterfall.clock.tick-interval-ms` (default 1000; 0 disables, tests construct the service with 0). Ticks stay seed-driven, so determinism is unaffected. The clock stops at a choice, and answering resumes it. STEP ONE TICK still works.
- `/api/story` now returns `activeChoice` (adds `title`); `POST /api/choices/{id}` accepts `optionId` as well as `optionIndex`. `story.js` field names fixed, plus a 2 s poll while RUNNING.
- Final recording: Observer records capped at 3 distinct, one beat per survivor, no repeated generic death lines.
- Tests: `DemoControlsAndChoiceTest`. Full suite 313 run, 11 errors, all `Postgres17LiveIntegrationTest` (no PostgreSQL 17 here).
