# AfterFall Development Progress

This file is the handoff point between Claude Code sessions. Update it after every completed task. Keep it factual and concise.

## Current status
- Project: AfterFall - The Last Days of Humanity
- Canonical specification: `docs/blueprint/AfterFall-Blueprint-Revised-Solo.md`
- Current milestone: M2 complete (Behaviors, Actions, Strategy and Command patterns, deterministic evaluation; ready for M3)
- Application code: Spring Boot skeleton plus human, regional, settlement domain models, autonomous behaviors, and action commands (no simulation ticking or threading yet)
- Last updated: 2026-11-20

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

## Milestone tracker
| Milestone | Scope | Status |
|---|---|---|
| Environment | JDK, Git, PostgreSQL, VS Code, workspace | Mostly complete; verify extensions and Git remote |
| M0 | Spring Boot project setup, packages, initial UML | Complete (2026-11-19) |
| M1 | Domain model and OOP pillars | Complete (Sessions 1 & 2 + final-review corrections; 2026-11-20) |
| M2 | Behaviours, actions and core patterns | Complete (2026-11-20) |
| M3 | Correct single-threaded simulation baseline | Not started |
| M4 | Four workers, deterministic merge, RNG, migration and concurrency | Not started |
| M5 | Resources and Famine/Disease crises | Not started |
| M6 | ObserverAI with four MUST policies | Not started |
| M7 | EventBus and Narrator with 40+ templates | Not started |
| M8 | Choices, consequences and four threshold-driven chapters | Not started |
| M9 | Spring Boot, PostgreSQL, persistence interfaces and async recorder | Not started |
| M10 | Story-first frontend: four core pages plus operator controls | Not started |
| M11 | Recovered records and final recap | Not started |
| M12 | Integration, tests, UML, docs and checkpoint demo | Not started |
| M13 | Feature freeze, bug bash, three rehearsals and fallback recording | Not started |
| M14 (SHOULD) | Audience voting | Not started |
| M15 (SHOULD) | Scale to about 100 survivors | Not started |
| M16 (SHOULD) | Fifth chapter, more records, imagery and optional map | Not started |

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
- None. M0, M1, and M2 completed and verified.
- Awaiting user approval to proceed to M3 (Correct single-threaded simulation baseline).

## Latest completed task: M2 - Autonomous Behaviours, Actions & Core Patterns (2026-11-20)
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

**Known limitations**: Simulation engine and ticking are not yet running (scheduled for M3).

## Next task
- M3: Correct single-threaded simulation baseline (`WorldClock`, single-threaded `TickCoordinator`, action execution pipeline). Start only after explicit user approval.

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

## Next task
- M1 Final Review & Approval -> Proceed to M2 (Behaviours, actions and core patterns). Start only after explicit user approval.

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

## Next task
- M1 Session 2: Region, Settlement/World, initial population (20-30, configurable). Start only after explicit user approval.

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
- 2026-11-20: M1 Session 2 implemented and verified (Region, Settlement, World, PopulationConfig, InitialPopulation, class-diagram.mmd; 88 total passing tests).
- 2026-11-20: M1 Session 1 four targeted domain-model corrections implemented and verified (68 total passing tests).
- 2026-11-19: M1 Session 1 implemented and verified (human domain model, 63 new tests; 65 total passing).
- 2026-11-19: M0 implemented and verified (Spring Boot 4.1.2 skeleton, package boundaries, boundary test, initial component diagram, README build commands).
- 2026-11-18: Environment setup guided; JDK 21 and PostgreSQL 17.12 verified; `afterfall` database created; VS Code workspace and Java extension pack prepared; private GitHub repository created; revised solo blueprint adopted as source of truth.
