# AfterFall: The Last Days of Humanity

A multithreaded, object-oriented interactive storytelling simulation of an AI-manipulated post-apocalyptic society. Built as an advanced solo project for **Advanced Object-Oriented Programming (AOOP)** using Java 21, Spring Boot, and PostgreSQL 17.

---

## 1. Project Overview

### Concept & Lore
In a desolate post-collapse world, twenty to thirty autonomous human survivors gather in a fragile settlement to rebuild society. Unknown to them, an automated surveillance and governance system known as the **Observer** monitors every biometric indicator, interpersonal conversation, and resource transaction. 

Rather than physically intervening, the Observer subtly manipulates information, amplifies resource scarcity, and erodes trust between survivors. The simulation core deterministically simulates survivor needs, daily actions, and crises. An event-driven **Narrator** converts real simulation events into an authored documentary stream. 

The player assumes the role of the **Humanity Overseer**: an operator monitoring the settlement via an atmospheric terminal, inspecting character dossiers, making critical moral choices at pivotal moments, and discovering forensic evidence of the Observer's hidden manipulation.

The central thematic statement of AfterFall is:
> *"The cruelest destruction is the kind that convinces people they chose it themselves."*

### Narrative Arc: Four Chapters
The simulation progresses through four threshold-driven chapters, governed by state conditions (trust, morale, resources, population, and discovered evidence) rather than fixed tick boundaries, safeguarded by minimum dwell ($D_{\min}$), soft pressure escalation ($D_{\text{soft}}$), and hard backstop ($D_{\text{hard}}$) guardrails:

- **Chapter I: A New Beginning**  
  *Normal Gameplay Thresholds:* Baseline settlement survival. Transition to Chapter II fires after minimum dwell ($D_{\min} \ge 8$ ticks) when communal trust drops below $T_1 \le 50$, or resource shortage persists for $N_1 \ge 4$ consecutive ticks, with a hard backstop forcing transition at $D_{\text{hard}} = 48$ ticks. (In compressed demonstration profiles, thresholds can be tuned for rapid pacing).  
  *Atmosphere:* Fragile hope, basic foraging routines, shelter repairs, resting, and early community bonds.
- **Chapter II: The Fracture**  
  *Normal Gameplay Thresholds:* Transition to Chapter III requires minimum dwell ($D_{\min} \ge 8$ ticks) and is triggered when discovered evidence reaches $E \ge 2$, Observer exposure reaches $X \ge 60$, or both morale and trust collapse below $M_1 \le 40$ and $T_2 \le 40$ (hard backstop at $D_{\text{hard}} = 48$ chapter ticks).  
  *Atmosphere:* Resource scarcity intensifies. The Observer begins actively sowing doubt; interpersonal tension increases, and factions quietly divide.
- **Chapter III: The Revelation**  
  *Normal Gameplay Thresholds:* Transition to Chapter IV triggers after minimum dwell when active population drops below $P \le 15$ survivors, or critical conditions (morale $M_2 \le 25$ and trust $T_3 \le 25$) persist for $N_2 \ge 6$ ticks (hard backstop at $D_{\text{hard}} = 48$ chapter ticks).  
  *Atmosphere:* The turning point. The player investigates and discovers suppressed records of Observer tampering. Survivors confront escalating crises (famine and contagion).
- **Chapter IV: The Last Recording**  
  *Atmosphere & Resolution:* The settlement's final struggle and eventual collapse. Concludes with a forensic **RunRecap** and documentary summary, culminating in the mandatory closing record:
  > *"The settlement fell silent. The Observer remains active."*

### V1 Scope
AfterFall V1 is a deterministic single-player simulation designed for complete local execution. It requires no cloud dependencies or external LLM connections.

---

## 2. Technology Stack

- **Core Language:** Java 21 LTS (Records, sealed interfaces, pattern matching, `SplittableRandom`).
- **Application Framework:** Spring Boot 4.1.2 (Lifecycle management, dependency injection, application configuration).
- **Relational Database:** PostgreSQL 17 (Relational persistence for simulation runs, canonical events, choices, interventions, and evidence).
- **ORM & Data Access:** Spring Data JPA / Hibernate (with zero imports inside the domain or simulation packages).
- **Concurrency & Multithreading:** Java Concurrency Utilities (`java.util.concurrent`), including custom `ThreadPoolExecutor`, `CountDownLatch`, `AtomicBoolean`, `Future`, and `ReentrantLock`.
- **Build System:** Maven Wrapper (`mvnw.cmd` / `mvnw`).
- **Frontend Presentation:** Vanilla HTML5, CSS3 (Design Tokens, responsive grid/flexbox), and modern vanilla JavaScript (Fetch API + STOMP over SockJS).
- **Testing & Quality Assurance:** JUnit 5, AssertJ, Mockito.

---

## 3. Architecture

AfterFall follows a strict **Clean / Hexagonal Layered Architecture** with unidirectional dependency flow. The simulation engine and domain core are entirely decoupled from Spring, JPA, Hibernate, and database drivers.

```
+-----------------------------------------------------------------------------------+
|                        PRESENTATION / BROWSER LAYER                               |
|   index.html (Prologue)  |  story.html (Observation)  |  people.html (Dossiers)   |
|                 records.html (Forensic Comparator & Run Recap)                    |
+-----------------------------------------------------------------------------------+
                                        | (HTTP REST & STOMP / WebSocket)
                                        v
+-----------------------------------------------------------------------------------+
|                         WEB & CONTROLLER LAYER (Spring)                           |
|  SimulationApiController  |  StoryApiController  |  ObserverApiController         |
|  MetricsApiController     |  WebSocketConfig     |  WebSocketBroadcaster          |
+-----------------------------------------------------------------------------------+
                                        |
                                        v
+-----------------------------------------------------------------------------------+
|                         APPLICATION LAYER (Use Cases & Ports)                     |
|  SimulationService (Orchestrator)                                                 |
|  Persistence Ports: RunRepository | EventRecorder | EvidenceDiscoveryRepository   |
+-----------------------------------------------------------------------------------+
                  |                                          |
                  v (Dependency Inversion)                   v
+-----------------------------------------+   +-------------------------------------+
|      SIMULATION & DOMAIN CORE           |   |       INFRASTRUCTURE LAYER          |
|  - World, Settlement, Region, Survivor  |   |  - AsyncEventRecorder               |
|  - SimulationEngine & WorldClock        |   |  - JpaPostgresRunRepository         |
|  - 4 RegionWorkers & RegionWorkerPool   |   |  - JpaEventRecorder                 |
|  - TickCoordinator (Sole Writer Merge)  |   |  - JpaEvidenceDiscoveryRepository   |
|  - ObserverAI (4 Manipulation Policies) |   |  - In-Memory Fallback Adapters      |
|  - EventBus & Narrator (46 Templates)   |   |  - PostgreSQL 17 Database           |
|  - ChoiceCoordinator & ChapterManager   |   +-------------------------------------+
|  - EvidenceService & RecapGenerator     |
+-----------------------------------------+
```

### Four-Worker Concurrency & Deterministic Merge
To achieve high throughput without race conditions or nondeterministic outcomes, AfterFall implements a strict **4-Worker Partitioned Evaluation Model**:

1. **Immutable Snapshot (`WorldView`):**  
   At the start of every tick, the `TickCoordinator` snapshots the mutable settlement into an immutable `WorldView`.
2. **Parallel Regional Evaluation:**  
   The settlement is divided into four geographic regions: `NORTH`, `SOUTH`, `EAST`, and `WEST`. Four dedicated worker threads in a `RegionWorkerPool` evaluate survivor behaviors concurrently. Workers evaluate autonomous actions using seeded PRNGs derived from `(runSeed, regionId, tick)` and return immutable `RegionResult` objects containing proposed actions, resource claims, and migration requests.
3. **No Direct Mutation:**  
   Region workers never mutate shared settlement state, resource stocks, or relationships directly.
4. **Deterministic Sole-Writer Merge:**  
   The `TickCoordinator` collects all worker futures and executes a single-threaded merge in canonical order:
   - Sorts results by `RegionId` (1..4) and `SurvivorId`.
   - Validates and applies cross-region migration requests via `MigrationCoordinator` and `MigrationHandoff`.
   - Atomically applies survivor action deltas via `StateChangeApplier.applyBatch` (with automatic rollback on failure).
   - Resolves resource claims deterministically against `ResourceStock`.
   - Advances active crises (`Famine`, `Disease`) via `CrisisManager`.
   - Applies queued Observer interventions at the safe boundary.
   - Applies queued player choice consequences at the safe boundary.
   - Evaluates chapter progression rules.
   - Fans out canonical `SimEvent`s to the `EventBus`.
   - Advances the `WorldClock` and increments world version.

```
Tick Start
    │
    ▼
[Immutable WorldView Snapshot]
    │
    ├───────────────┬───────────────┬───────────────┐
    ▼               ▼               ▼               ▼
Region NORTH    Region SOUTH    Region EAST     Region WEST
 Worker 1        Worker 2        Worker 3        Worker 4
    │               │               │               │
    └───────────────┴───────┬───────┴───────────────┘
                            ▼
           [Collect & Sort by Region + Survivor]
                            │
                            ▼
     [TickCoordinator: Sole-Writer Deterministic Merge]
      1. Cross-Region Migration Handoff
      2. Atomic Survivor Action Batch Application
      3. Resource Claim Resolution
      4. Crisis Advancement (Famine / Disease)
      5. Observer Intervention Application
      6. Choice Consequence Application
      7. Chapter Transition Check
      8. Canonical SimEvent Fan-Out (EventBus)
                            │
                            ▼
           [Clock Advance & Safe Pause Check]
```

---

## 4. Major Systems

### 1. Survivor Behavior & Action System
- **Needs & Biometrics:** Survivors possess calibrated needs (`HUNGER`, `THIRST`, `REST`, `SAFETY`, `SOCIAL`) tracked on a `0–100` scale, a `Vitality` score (`0–100`), and a medical `Condition` (`HEALTHY`, `MALNOURISHED`, `DEHYDRATED`, `INFECTED`, `CRITICAL`, `DECEASED`).
- **Autonomous Action Decider:** Evaluates needs, personality traits, and regional conditions to select autonomous actions: `FORAGE_FOOD`, `PUMP_WATER`, `SCAVENGE_MEDICINE`, `REST`, `SOCIALIZE`, or `RELOCATE`.
- **Command Pattern Execution:** Actions emit `ActionResult` deltas and `ProposedEvent` items without mutating the survivor directly during worker turns.

### 2. Resources & Crisis Management
- **Thread-Safe Resource Stocks:** Shared settlement stock pools for `FOOD`, `WATER`, and `MEDICINE`, protected against race conditions by `ReentrantLock` wrappers.
- **Crises (Template Method Pattern):**
  - **Famine:** Triggered when food stock falls below subsistence thresholds; causes accelerated hunger accumulation, malnutrition, and eventual starvation.
  - **Disease Outbreak:** Triggered by poor sanitation or low vitality; introduces infectious contagion spreading through interpersonal contact and requiring medicine consumption.

### 3. ObserverAI & Manipulation Policies
The Observer is an automated governance agent operating on the **Strategy Pattern**. It evaluates immutable settlement metrics and applies four core policies:
- **`InformationDistortionPolicy`:** Fabricates reports of resource theft or hoarding, driving paranoia and reducing communal safety perception.
- **`TrustErosionPolicy`:** Identifies close interpersonal relationships and selectively plants suspicion, eroding mutual trust scores.
- **`ResourcePressurePolicy`:** Amplifies perceived scarcity when supplies are running low, inducing panic consumption and territorial hoarding.
- **`SelectiveDisclosurePolicy`:** Discloses alarming infection warnings while concealing available medical cures to drive panic.

Every intervention preserves an unalterable audit record containing the `originalReport`, `alteredReport`, `policy`, and `target` for forensic investigation.

### 4. EventBus & Template-Driven Narrator
- **Asynchronous Fan-Out:** The `EventBus` delivers canonical `SimEvent` instances to subscriber queues (`Narrator`, `AsyncEventRecorder`) without blocking the simulation engine.
- **Documentary Narrator:** An independent subscriber that evaluates event severity, enforces an 8-event anti-repetition memory buffer, and selects from **51 authored narrative templates** across routines, crises, Observer manipulations, deaths, choices, and chapter transitions using a deterministic seeded selector.
- **Ethical Content Standards:** Enforces a zero-tolerance policy against graphic or gratuitous violence, using quiet, understated documentary prose.

### 5. Choices & Safe Pause Coordination
- **`ChoiceMoment`:** Authored moral and logistical dilemmas presented to the Overseer (e.g., rationing medicine, isolating infected survivors, or investigating secret broadcasts).
- **Safe Tick-Boundary Pause:** When a choice is triggered, the simulation safely completes the current tick and transitions to `SimulationLifecycle.PAUSED`.
- **Consequence Queueing:** The player submits an option via REST; consequences are queued and deterministically applied by the `TickCoordinator` during Phase 5g of the following tick. If an invalid option is provided, the system falls back safely to an authored default option.

### 6. Evidence Discovery & Forensic Comparator
- **Recovered Records:** Interventions generate forensic `EvidenceItem` records containing the discrepancy between the ground truth (`originalReport`) and the sanitized Observer report (`alteredReport`).
- **Explicit Investigation:** Evidence remains hidden until the player investigates it on `records.html`, incrementing the chapter evidence discovery counter with strict duplicate prevention.

### 7. Final Recording & RunRecap
- Upon settlement collapse or run termination, the `RecapGenerator` produces a comprehensive, fact-derived post-mortem documentary.
- Includes total duration, mortality statistics, dominant causes of death, decisive player choices, full Observer intervention audits, and discovered evidence discrepancies.
- Concludes with the unalterable system closing line:  
  `"The settlement fell silent. The Observer remains active."`

### 8. Persistence & AsyncEventRecorder
- **Dual Persistence Architecture:** Strict persistence interfaces (`RunRepository`, `EventRecorder`, `EvidenceDiscoveryRepository`) implemented by both PostgreSQL JPA adapters and in-memory fallback adapters.
- **`AsyncEventRecorder`:** Implements the **Producer-Consumer Pattern** with a dedicated background writer thread and batch flusher, offloading database I/O from the simulation loop.
- **Crash Recovery:** Run state, ordered event history, choice logs, and discovered evidence can be fully reloaded across application restarts to reconstruct identical `RunRecap` summaries.

---

## 5. Object-Oriented Programming & Design Patterns

AfterFall was designed to exemplify clean object-oriented architecture and design patterns:

### Four Pillars of OOP
1. **Encapsulation:**
   - Domain invariants are rigorously enforced. For example, `Resource` ensures quantities never fall below zero using private locks and checked boundary methods. `SurvivorNeeds` encapsulates hunger/thirst/health metrics and rejects illegal negative adjustments.
2. **Abstraction:**
   - Persistence operations are abstracted behind domain interfaces (`RunRepository`, `EventRecorder`, `EvidenceDiscoveryRepository`). The simulation engine communicates strictly via these abstractions without knowledge of JPA or SQL.
3. **Inheritance:**
   - Used purposefully in the crisis framework: abstract base class `Crisis` defines core duration tracking, active state flags, and the template execution lifecycle inherited by `FamineCrisis` and `DiseaseOutbreakCrisis`.
4. **Polymorphism:**
   - Demonstrated in Observer policies (`InterventionPolicy`), where `InformationDistortionPolicy`, `TrustErosionPolicy`, `ResourcePressurePolicy`, and `SelectiveDisclosurePolicy` are evaluated polymorphically by `ObserverAI`.

### SOLID Principles
- **Single Responsibility Principle (SRP):**  
  `TickCoordinator` coordinates tick execution; `MigrationCoordinator` handles relocation validation; `StateChangeApplier` applies survivor mutations; `Narrator` renders story text.
- **Open/Closed Principle (OCP):**  
  New Observer policies can be introduced by implementing `InterventionPolicy` without modifying `ObserverAI`. New narrative templates can be added to `NarrativeTemplateRepository` (currently providing 51 templates) without changing `Narrator`.
- **Liskov Substitution Principle (LSP):**  
  Both `JpaPostgresRunRepository` and `InMemoryRunRepository` satisfy the exact same `RunRepository` contract and can be swapped seamlessly in tests or production.
- **Interface Segregation Principle (ISP):**  
  Persistence interfaces are segregated by domain concern: `EventRecorder` handles event appending, `RunRepository` handles run lifecycle and recaps, and `EvidenceDiscoveryRepository` tracks forensic evidence.
- **Dependency Inversion Principle (DIP):**  
  High-level simulation modules depend on persistence abstractions located in `com.afterfall.application.persistence`. Low-level infrastructure adapters in `com.afterfall.infrastructure` implement these interfaces.

### GoF & Concurrency Design Patterns
| Pattern | Concrete Implementation | Purpose |
|---|---|---|
| **Strategy** | `InterventionPolicy` (`InformationDistortionPolicy`, etc.) | Encapsulates distinct Observer manipulation heuristics. |
| **Observer** | `EventBus` and `EventSubscriber` | Fans out canonical `SimEvent`s to decoupled consumers (`Narrator`, `AsyncEventRecorder`). |
| **Command** | `SurvivorAction` and `Intervention` | Encapsulates proposed actions and manipulations as objects to be validated and executed later. |
| **Template Method** | `Crisis.advance(Settlement, ResourceStock)` | Outlines the invariant skeleton of crisis progression while delegating specific effects to subclasses. |
| **Builder** | `SimEvent.builder()`, `TickResult.builder()` | Constructs complex immutable domain and simulation result objects with clear, fluent validation. |
| **Producer-Consumer** | `AsyncEventRecorder` | Uses a thread-safe queue and dedicated background writer thread to batch-persist simulation events. |
| **State Machine** | `SimulationEngine` (`SimulationLifecycle`) | Manages valid transitions between `STOPPED`, `STARTING`, `RUNNING`, `PAUSE_REQUESTED`, `PAUSED`, and `STOPPING`. |

---

## 6. Installation & Local Execution

AfterFall V1 runs entirely locally on Windows. Once the repository is cloned and Maven dependencies are cached, it operates completely offline without internet access.

### Prerequisites
1. **Operating System:** Windows 10/11 (64-bit).
2. **Java Development Kit:** Eclipse Temurin JDK 21 LTS installed and set on your environment path:
   ```powershell
   java -version
   # Verify output reports openjdk version "21.x.x"
   ```
3. **Database:** PostgreSQL 17 installed and running locally as a Windows service (`postgresql-x64-17` on port `5432`).

### Database Setup
1. Open PowerShell and connect to PostgreSQL via `psql` (or pgAdmin 4):
   ```powershell
   psql -U postgres -h localhost -p 5432
   ```
2. Create the AfterFall database:
   ```sql
   CREATE DATABASE afterfall;
   \q
   ```

### Environment Configuration
Configure your PostgreSQL credentials via environment variables in your PowerShell session matching `application.yml`:
```powershell
$env:AFTERFALL_DB_URL="jdbc:postgresql://localhost:5432/afterfall"
$env:AFTERFALL_DB_USER="postgres"
$env:AFTERFALL_DB_PASSWORD="your_postgres_password"
```

### Build & Run Commands
All build and run operations use the Maven Wrapper from the `backend/` directory:

```powershell
# Navigate to backend directory
cd backend

# 1. Execute complete test suite (300 tests)
.\mvnw.cmd test

# 2. Package into an executable JAR
.\mvnw.cmd package

# 3. Launch application
java -jar target\afterfall-backend-0.0.1-SNAPSHOT.jar
```

Alternatively, you can run directly via Spring Boot:
```powershell
.\mvnw.cmd spring-boot:run
```

### Accessing the Web Interface
Once the terminal displays `Started AfterFallApplication in X.XXX seconds`, open your browser:
- **Observation Deck & Prologue:** [http://localhost:8080/](http://localhost:8080/)
- **Live Chronicler & Simulation Controls:** [http://localhost:8080/story.html](http://localhost:8080/story.html)
- **Survivor Directory & Dossiers:** [http://localhost:8080/people.html](http://localhost:8080/people.html)
- **Recovered Records & Final Recap:** [http://localhost:8080/records.html](http://localhost:8080/records.html)

---

## 7. Testing & Quality Assurance

AfterFall maintains a comprehensive automated testing suite ensuring deterministic execution, thread safety, and data integrity.

### Verified Test Suite Baseline
- **Total Tests Run:** **300**
- **Failures:** **0**
- **Errors:** **0**
- **Skipped:** **0**

### Key Test Categories
1. **Architecture & Boundaries:**
   - `CoreBoundaryTest`: Scans `com.afterfall.domain` and `com.afterfall.simulation` and fails the build if any class imports Spring, JPA, Hibernate, or PostgreSQL.
2. **Concurrency & Thread Safety:**
   - `WorkerJitterDeterminismTest`: Injects synthetic random delays (0–50 ms) across region worker threads. Proves that thread scheduling jitter cannot alter seeded event logs, survivor states, or final state hashes.
   - `CleanShutdownTest`: Verifies the clean termination of `RegionWorkerPool`, `EventBus` subscriber executors, `AsyncEventRecorder` writer threads, and `SimulationEngine` without leaked daemon threads.
   - `UnsafeRaceDemo`: Benchmark demonstrating shared-state corruption under unsynchronized multithreading versus AfterFall's lock-protected resources and sole-writer merge.
3. **End-to-End Lifecycle & Restart Recovery:**
   - `FullRunLifecycleEndToEndTest`: Exercises the full gameplay arc: autonomous ticks, canonical event publication, Observer intervention, tick-boundary choice pausing, decision submission, chapter transition, evidence discovery, final recording synthesis, simulated application restart, and identical `RunRecap` regeneration from PostgreSQL.
4. **Domain & Mechanics:**
   - Unit tests covering human needs, health, traits, relationships, resource stocks, famine/disease crises, and deterministic PRNG streams.
5. **Narrative & Content Policy:**
   - `NarrativeTemplateRepositoryTest`: Tests all 51 narrative templates and executes a **Banned-Term Keyword Audit** to verify 0 sensitive or graphic expressions.
6. **Web & REST Integration:**
   - `WebControllersTest`: Verifies all REST endpoints (`/api/state`, `/api/sim/*`, `/api/survivors`, `/api/story`, `/api/choices/*`, `/api/records`, `/api/metrics`) and WebSocket/STOMP broadcasting.

To run tests:
```powershell
cd backend
.\mvnw.cmd test
```

---

## 8. UML & Architectural Diagrams

All architectural diagrams are maintained as Mermaid source files in [`docs/uml/`](file:///c:/Users/user/Projects/AfterFall/docs/uml):

- **Component Diagram:** [`docs/uml/component-diagram.mmd`](file:///c:/Users/user/Projects/AfterFall/docs/uml/component-diagram.mmd) — Five-tier component diagram documenting the browser, REST controllers, STOMP broadcaster, `SimulationService`, core simulation engine, dual persistence adapters, and PostgreSQL.
- **Tick Sequence Diagram:** [`docs/uml/tick-sequence.mmd`](file:///c:/Users/user/Projects/AfterFall/docs/uml/tick-sequence.mmd) — Step-by-step sequence of `TickCoordinator.doExecute()`, from snapshot creation through parallel evaluation, migration handoff, sole-writer merge, and event fan-out.
- **Decision Sequence Diagram:** [`docs/uml/decision-sequence.mmd`](file:///c:/Users/user/Projects/AfterFall/docs/uml/decision-sequence.mmd) — Complete sequence of choice moment triggers, tick-boundary pausing, REST decision submissions, default fallbacks, and next-tick consequence application.
- **Cross-Region Handoff Diagram:** [`docs/uml/cross-region-handoff.mmd`](file:///c:/Users/user/Projects/AfterFall/docs/uml/cross-region-handoff.mmd) — Relocation validation, roster mutation, location index updates, and atomic rollback mechanics.
- **Class Diagram:** [`docs/uml/class-diagram.mmd`](file:///c:/Users/user/Projects/AfterFall/docs/uml/class-diagram.mmd) — Comprehensive structural class diagram of domain entities, value objects, and behavior interfaces.
- **Concurrency Architecture:** [`docs/uml/concurrency-architecture.mmd`](file:///c:/Users/user/Projects/AfterFall/docs/uml/concurrency-architecture.mmd) — Thread boundaries, snapshot isolation, and sole-writer merge architecture.
- **Lifecycle State Machine:** [`docs/uml/lifecycle-state.mmd`](file:///c:/Users/user/Projects/AfterFall/docs/uml/lifecycle-state.mmd) — Simulation engine lifecycle states and valid transitions.

---

## 9. Current Limitations & Scope Boundaries

### Implemented in V1 (Locked Scope)
- 20–30 configurable survivors in a 4-region settlement.
- 4-worker multithreaded simulation engine with deterministic sole-writer merge.
- Calibrated survivor biometrics and autonomous action decider.
- Resource stocks (Food, Water, Medicine) and Famine/Disease crises.
- ObserverAI with 4 manipulation policies and tamper-evident audit trail.
- EventBus with decoupled fan-out to Narrator and persistence.
- Template-driven Narrator with 51 authored variants across all event types.
- Four threshold-driven chapters with safe choice pausing and consequence application.
- Forensic evidence discovery and post-mortem documentary `RunRecap`.
- Story-first web frontend across 4 responsive HTML screens.
- PostgreSQL 17 persistence with asynchronous recording.

### Explicitly Excluded from V1 (Future / Optional Scope)
- **External LLM Integration:** Narration is generated deterministically from authored templates to ensure speed, consistency, and zero external runtime dependencies.
- **Multiplayer / Audience Voting (M14):** Single-player Overseer experience only.
- **100+ Survivor Scale-Up (M15):** Tuned for 20–30 survivors to prioritize deep character dossiers and legible social dynamics.
- **Interactive Settlement Map / Sound FX (M16):** Atmospheric UI uses quiet cinematic minimalism and textual HUD presentation without canvas maps or audio playback.
- **Independent GuardianAI:** The player is the sole overseer monitoring the settlement against Observer manipulation.

---

## 10. Repository Layout & Governance

```
AfterFall/
├── backend/
│   ├── src/main/java/com/afterfall/
│   │   ├── application/       # Orchestrator (SimulationService) & persistence ports
│   │   ├── chapter/           # ChapterManager & transition rules
│   │   ├── choice/            # ChoiceCoordinator, ChoiceMoment & consequences
│   │   ├── config/            # Spring configuration & WebSocket/STOMP setup
│   │   ├── domain/            # Plain Java domain model (World, Survivor, Resources, Crises)
│   │   ├── events/            # Canonical SimEvent, EventBus & EventSubscriber
│   │   ├── infrastructure/    # JPA & In-memory adapters, AsyncEventRecorder
│   │   ├── narrator/          # Narrator, StoryCaption & 46 authored templates
│   │   ├── observer/          # ObserverAI & 4 manipulation policies
│   │   ├── recap/             # EvidenceService, FinalRecording & RecapGenerator
│   │   ├── simulation/        # SimulationEngine, TickCoordinator & 4 RegionWorkers
│   │   └── web/               # Spring REST controllers & DTOs
│   ├── src/main/resources/
│   │   ├── static/            # Frontend: index.html, story.html, people.html, records.html, CSS, JS
│   │   └── application.properties
│   └── src/test/java/com/afterfall/  # 300 automated unit, integration, and concurrency tests
├── docs/
│   ├── blueprint/             # Canonical solo blueprint specification
│   ├── architecture/          # Approved technical clarifications
│   ├── desgin/                # Visual direction and UI design tokens
│   └── uml/                   # Mermaid architecture, sequence, and class diagrams
├── CLAUDE.md                  # Development instructions & architecture rules
├── PROGRESS.md                # Milestone tracker & session handoff log
└── README.md                  # Comprehensive project documentation
```
