# AfterFall Rehearsal Pack (M13 Phase 5)

Companion to `DEMO_SCRIPT.md` (the timed 7-minute script and screen cues live there). This file adds the rehearsal procedure and a wider Q&A bank for the teachers' questions. Rehearsals must be spoken aloud by the presenter; nobody can do them for you.

## 1. Three rehearsal passes
| Pass | Goal | Rules | Done |
|---|---|---|---|
| 1 | Learn the flow | Script in hand, app running, stop and fix anything unclear | [ ] |
| 2 | Time it | No script on screen, phone timer, must finish between 6:30 and 7:00 | [ ] |
| 3 | Pressure test | Another person asks 3 random questions from section 3, one deliberate failure from the fallback matrix (stop PostgreSQL, or refresh the browser) | [ ] |

After each pass note: time, the sentence you stumbled on, one question you could not answer. Fix those first.

## 2. Pre-rehearsal checklist
- [ ] PostgreSQL service running, `AFTERFALL_DB_PASSWORD` set (see `DEMO_SCRIPT.md` section 2)
- [ ] `.\mvnw.cmd test` was green today (306 tests with PostgreSQL 17)
- [ ] App started, `http://localhost:8080/` loads, WS pill shows live
- [ ] Browser full-screen (F11), zoom 100 percent, notifications off
- [ ] Offline mode tried once: `--spring.profiles.active=inmemory` starts with PostgreSQL stopped
- [ ] Printed UML diagrams (docs/uml) and the fallback video on a USB stick
- [ ] Laptop charged, charger packed, second display cable if needed

## 3. Question bank (short answers; know the class names)
**OOP**
- *Encapsulation?* `Resource` keeps quantity private behind a lock and never goes negative; `Inventory` rejects over-capacity and leaves state unchanged on failure.
- *Abstraction?* `RunRepository`, `EventRecorder`, `EvidenceDiscoveryRepository`: the core only knows these interfaces.
- *Inheritance, and why so little?* Only the genuine is-a case: abstract `Crisis` with `FamineCrisis` and `DiseaseOutbreakCrisis`. `Survivor` uses composition (Needs, Health, Personality, Inventory, Profession) because it is has-a, not is-a.
- *Polymorphism?* `ObserverAI` calls every `InterventionPolicy` the same way; `BehaviorSelector` calls `Behavior` strategies; `Action` implementations are evaluated through one interface.

**SOLID** (one concrete class each)
- S: `TickCoordinator` merges, `StateChangeApplier` applies, `Narrator` writes text.
- O: add a fifth Observer policy by implementing `InterventionPolicy`; `ObserverAI` is unchanged.
- L: `InMemoryRunRepository` and `JpaPostgresRunRepository` pass the same contract tests.
- I: persistence is split into three small interfaces, not one big one.
- D: `SimulationService` depends on `RunRepository`; Spring wires the adapter (`PersistenceConfiguration`).

**Patterns**: Strategy (`Behavior`, `InterventionPolicy`), Command (`Action`, `Intervention`), Observer (`EventBus`/`EventSubscriber`), Template Method (`Crisis`), State (`SimulationLifecycle`), Facade (`SimulationEngine`). Producer-consumer (`AsyncEventRecorder`) and immutable snapshots (`WorldView`) are concurrency/data-sharing techniques, not GoF patterns.

**Threading and determinism**
- *Why is it deterministic with 4 threads?* Workers see an immutable `WorldView`, own disjoint survivors and return an immutable `RegionResult`. Only `TickCoordinator` writes, merging in fixed order (region, survivor, action sequence). RNG is seeded from run seed, region and tick. Completion order never matters.
- *Proof?* `WorkerJitterDeterminismTest` adds 0 to 50 ms random delays and still gets identical events and state hash.
- *Race on shared data?* Workers never mutate shared state; the `Resource` race test covers direct concurrent access.
- *Worker throws?* The tick fails as a whole, nothing is partially merged (`TickCoordinatorFailureTest`).
- *Threads leaking?* `CleanShutdownTest` checks pool, channel and recorder threads end.
- *Pause?* The current tick finishes and merges first, then the engine enters `PAUSED`.

**Persistence and honesty**
- *PostgreSQL down?* Recorder switches to in-memory and reports it. Whole-app offline mode: `inmemory` profile (process-local, not crash-durable).
- *Known limit?* A hard JVM kill does not resume mid-tick; persisted history and recap survive, the engine restarts from its persisted state.
- *Is the AI an LLM?* No: `ObserverAI` is deterministic and rule-based; the Narrator uses 51 authored templates and only reports real events.
- *Where is the tragedy decided?* The ending is fixed in tone, but chapter transitions are threshold-driven from simulation state.

## 4. If you freeze
Say what the screen shows, then continue at the next cue. Never apologise for the app; use the fallback matrix in `DEMO_SCRIPT.md` section 4.
