# AfterFall: OOP, SOLID, Patterns and Threading Map

One page for the AOOP viva. Every row names a real class under `backend/src/main/java/com/afterfall/` and the test that checks it. Paths are relative to that folder. Diagrams are in [`docs/uml`](../uml/README.md).

## 1. The four pillars

| Pillar | Where to point | What to say |
|---|---|---|
| **Encapsulation** | `domain/Resource` | Quantity is private. Every change goes through a `ReentrantLock` and a check, so it can never go negative. `ResourceTest.raceConditionTestQuantityNeverNegative` hammers one `Resource` from many threads. |
| | `domain/Needs`, `domain/Health`, `domain/Relationship` | Validate on every write. `Relationship` rejects history entries older than the last one before changing anything. |
| **Abstraction** | `application/persistence/EventRecorder`, `RunRepository`, `EvidenceDiscoveryRepository` | The engine knows only these ports. It never sees JPA or SQL. |
| **Inheritance** | `domain/Crisis` with `FamineCrisis`, `DiseaseOutbreak` | The only class hierarchy, used for a real is-a relationship. Everything else uses composition. |
| **Polymorphism** | `observer/InterventionPolicy` (4 impls), `action/Action` (5 impls), `behavior/Behavior` (4 impls), `chapter/ChapterPolicy` (4 impls) | The caller holds the interface and never branches on the concrete type. |
| **Composition over inheritance** | `domain/Survivor` | Built from `Needs`, `Health`, `Personality`, `Profession`, `Inventory`, `Relationship` and `CharacterMemory`, not from a subclass tree. |

## 2. SOLID

| Principle | Class example | Evidence |
|---|---|---|
| **S**ingle responsibility | `TickCoordinator` runs the tick protocol. `MigrationCoordinator` validates moves. `StateChangeApplier` applies deltas. `Narrator` renders text. `CrisisManager` advances crises. | Each class has one reason to change. `SimulationEngine` only owns lifecycle and delegates the tick. |
| **O**pen/closed | `observer/ObserverAI` | Add a policy by implementing `InterventionPolicy`; `ObserverAI` is not edited. Narration grows by adding to `narrator/NarrativeTemplateRepository`. |
| **L**iskov substitution | `RunRepository`: `JpaPostgresRunRepository` and `InMemoryRunRepository` | `AbstractRunRepositoryContractTest` and `AbstractEventRecorderContractTest` run the same assertions against both adapters. |
| **I**nterface segregation | `EventRecorder` (append events), `RunRepository` (run lifecycle), `EvidenceDiscoveryRepository` (evidence), `EventSubscriber` (one callback) | A consumer depends only on the small interface it uses. |
| **D**ependency inversion | Ports in `application/persistence`, adapters in `infrastructure/persistence` | `config/PersistenceConfiguration` picks the adapter by `afterfall.persistence.mode`. `CoreBoundaryTest` fails the build if `domain` or `simulation` imports Spring, JPA, Hibernate or PostgreSQL. |

## 3. Design patterns

| Pattern | Class | Note |
|---|---|---|
| **Strategy** | `InterventionPolicy` (Observer policies), `Behavior` (survivor decisions), `ChapterPolicy` (chapter rules) | Three separate families, all swapped without touching the caller. |
| **Command** | `action/Action` returns an `ActionResult`; `observer/Intervention` is queued and applied later | The object describes the change. Only `TickCoordinator` applies it. |
| **Observer (publish/subscribe)** | `events/EventBus` and `EventSubscriber`: `Narrator`, `AsyncEventRecorder`, the caption listener | Each subscriber gets its own queue and thread, so a slow one cannot block the others or the tick. |
| **Template Method** | `Crisis.advance` is `final` and calls the abstract `checkTrigger` and `collectEffects` | The skeleton is fixed. Subclasses fill the steps. |
| **Facade** | `SimulationEngine` over the tick machinery; `SimulationService` over the whole core for REST | One simple entry point. |
| **State** | `simulation/SimulationLifecycle` enum with `canTransitionTo` | `RUNNING`, `PAUSE_REQUESTED`, `PAUSED`, `STOPPING`, `STOPPED`; `STOPPED` is terminal. See `lifecycle-state.mmd`. |
| **Factory** | `SeededRandomFactory.createForRegion(region, tick)` | One place derives every RNG from `(runSeed, region, tick)`. |
| **Builder** | `SimEvent.Builder` | Builds the immutable event with validation. |
| *Producer-consumer (concurrency, not GoF)* | `infrastructure/persistence/async/AsyncEventRecorder` | The tick produces. One writer thread consumes in batches. |
| *Immutable snapshot (data sharing, not GoF)* | `simulation/WorldView`, `RegionView`, `SurvivorView`, `RegionResult` | Workers read a frozen copy and return results instead of mutating. |

## 4. Threading and determinism

| Question | Answer | Where |
|---|---|---|
| How many threads run the simulation? | Four fixed region workers in one `ExecutorService`. No common pool. | `RegionWorkerPool` (`WORKER_COUNT = 4`) |
| What does a worker see? | An immutable `WorldView` for tick t. | `WorldView.snapshot` |
| What does a worker write? | Nothing shared. It returns a `RegionResult` with proposed actions, resource claims and migration requests. | `RegionWorker`, `RegionResult` |
| Who changes world state? | Only `TickCoordinator`, on the calling thread, after all four futures return. | `TickCoordinator.executeTick` |
| In what order does it merge? | Region ID, then survivor ID, then action sequence. Merge steps 5a to 5h: order, migrations, state batch, resource claims, crises, Observer interventions, choice consequences, chapter check. Then events are published and the clock advances. | `TickCoordinator` Phase 5 |
| What if a worker fails? | The tick is not merged. Validation happens in Phase 4, before any write. | `TickCoordinatorFailureTest` |
| Where does randomness come from? | `SplittableRandom` seeded from `(runSeed, region, tick)`. No `Math.random` or `ThreadLocalRandom`. | `SeededRandomFactory` |
| Can two ticks overlap? | No. An `AtomicBoolean` guard rejects re-entry. | `TickCoordinatorFailureTest.testCannotRunConcurrentTickViaReentrancyGuard` |
| How does a pause stay safe? | A choice requests a pause. The current tick finishes and merges, then the state becomes `PAUSED`. | `ChoicePauseResumeBoundaryTest` |
| Does recording block the tick? | No. Critical records go to a lossless queue and minor ones to a bounded queue, both drained by one writer thread. | `AsyncEventRecorder` |
| How do we prove it? | Same seed gives the same events and final state, also with random 0 to 50 ms delays injected into the workers. | `WorkerJitterDeterminismTest`, `SimulationDeterminismAndEquivalenceTest` |
| Do threads leak on shutdown? | Pool, EventBus subscribers and recorder thread all stop. | `CleanShutdownTest`, `AsyncRecorderReliabilityTest` |
| Chapters really threshold-driven? | 20 seeds (1001 to 1020), all four chapters in order, hard backstop never used. | `ChapterArcIntegrationTest` |

`demo/UnsafeRaceDemo` (test sources) is the deliberately unsafe comparison. It is kept out of the normal suite on purpose.

## 5. Likely viva questions

- **Why not let workers update shared state with locks?** Locks would make results depend on thread timing. A sole-writer merge in fixed order makes the run reproducible, and workers need no locks at all. `Resource` keeps its lock only as a safety net for direct concurrent access.
- **Why is the Observer not a thread of its own?** Its output must apply at a defined tick. `ObserverAI` reads a completed snapshot, and `TickCoordinator` applies its `Intervention` in step 5f, so scheduling cannot change the result.
- **Is the AI a language model?** No. It is deterministic and rule-based, and the Narrator only fills authored templates from real events.
- **What if PostgreSQL is missing?** Start with `--spring.profiles.active=inmemory`. Same ports, process-local storage.
