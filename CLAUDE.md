# AfterFall - Claude Code Project Instructions

## 1. Project identity
AfterFall: The Last Days of Humanity is a solo Advanced Object-Oriented Programming (AOOP) project. It is a multithreaded, object-oriented, interactive storytelling simulation of an AI-manipulated post-apocalyptic society.

The user is the Humanity Overseer. Survivors act autonomously. The Observer is a deterministic, rule-based fictional AI that manipulates information and social conditions. The Narrator turns real simulation events into documentary-style story scenes. The user observes, inspects, decides and investigates. The broad ending is tragic, while character outcomes and the path to the ending depend on simulation state and user decisions.

## 2. Source-of-truth order
Read `docs/blueprint/AfterFall-Blueprint-Revised-Solo.md` as the canonical project blueprint.

If instructions conflict, use this order:
1. The latest explicit user instruction.
2. This `CLAUDE.md` and `docs/architecture/TECHNICAL-CLARIFICATIONS.md` for approved engineering constraints.
3. The revised solo blueprint in `docs/blueprint/`.
4. Older plans or informal notes.

Revision 2 is authoritative over the earlier solo plan wherever they differ. Do not restore features cut from MUST. Do not expand scope without explicit user approval.

## 3. Confirmed environment and technology
- OS: Windows 10 Pro, 64-bit.
- Java: Eclipse Temurin JDK 21.
- Backend: Spring Boot (required).
- Database: PostgreSQL 17 (selected; local database name `afterfall`).
- Build: Maven; use the Maven Wrapper (`mvnw.cmd`) so a global Maven installation is not required.
- Frontend: HTML, CSS and JavaScript; Canvas is optional.
- Communication: REST and WebSocket/STOMP where required.
- Tests: JUnit 5 and Mockito where appropriate.
- Concurrency: Java ExecutorService, CompletableFuture/Future, BlockingQueue and locks as justified by the design.

Use stable, mutually compatible dependency versions. Do not invent or pin versions without checking compatibility. Do not add Docker, Redis, React, 3D tooling, an external LLM or unrelated dependencies unless the user explicitly approves them.

## 4. Locked MUST scope
The solo MUST scope is deliberately limited:
- 20-30 survivors initially; population must be configurable. Scaling to about 100 is SHOULD.
- Four processing regions and four region workers.
- WorldClock and TickCoordinator.
- Food, water and medicine resources.
- Two crises: Famine and Disease Outbreak.
- ObserverAI with four MUST policies: InformationDistortionPolicy, TrustErosionPolicy, ResourcePressurePolicy and SelectiveDisclosurePolicy.
- EventBus with independent subscriber fan-out.
- Narrator with at least 40 authored template variants.
- Four threshold-driven MUST chapters: A New Beginning, The Fracture, The Revelation and The Last Recording.
- Choice moments with safe pause/resume.
- PostgreSQL behind persistence interfaces plus an in-memory adapter.
- Asynchronous event recording.
- Story-first frontend: Opening, Story, People, Recovered Records and Final Recording.
- Metrics panel, four mandatory UML diagrams, README and tests.

Audience voting, a settlement map, approximately 100 survivors, a fifth chapter, additional crises, factions, trading, replay, sound and external LLM narration are not MUST. Never let optional work delay the MUST milestone.

## 5. Architecture boundaries
- Keep the simulation/domain core as plain Java. It must not import Spring, Spring MVC, WebSocket, JPA or PostgreSQL classes.
- Spring Boot is the application/infrastructure layer: REST, STOMP, dependency injection, configuration and adapters.
- The core depends on interfaces for persistence and messaging. Infrastructure supplies implementations.
- Prefer composition for `Survivor`: Needs, Health, Personality, Inventory, Profession/role, Behavior, relationships, memories, location and status.
- Use inheritance only for genuine is-a relationships, especially the abstract `Crisis` hierarchy.
- Keep classes focused. `SimulationEngine` is a Facade/coordinator, not a God class.
- Frontend code displays backend state and submits commands. It must not calculate authoritative survivor behaviour or Observer interventions.

## 6. Deterministic concurrency contract - non-negotiable
The simulation must be reproducible for the same run seed and the same ordered external decision/command log.

1. Each worker receives an immutable `WorldView` for tick t and the survivors it owns.
2. Workers do not mutate shared world state, another region's survivors, shared resources or shared relationships.
3. A worker returns an immutable `RegionResult` containing proposed state deltas for its own survivors, immutable events, resource claims, relationship deltas and migration requests.
4. `TickCoordinator` is the only writer/merger of world state. It waits for all workers, then merges in a fixed order: region ID, survivor ID, then action sequence within the survivor. Worker completion order must not affect results.
5. Resolve resource claims and relationship deltas deterministically during the merge. Cross-region migration is applied at the tick boundary; the survivor is processed by the destination region starting at tick t+1.
6. Use a deterministic seeded RNG derived from run seed, region ID and tick. Do not use `ThreadLocalRandom`, `Math.random()` or shared mutable RNG state in the simulation core.
7. Workers assign local event sequence identifiers; the coordinator assigns global sequence numbers during ordered merge. Narrator and recorder consume events in that order.
8. A worker failure must not result in a partial merge. Fail or pause the tick visibly and report the error.
9. External inputs (REST, STOMP, operator actions and optional votes) are validated, stamped and queued. Apply them only at a tick boundary in a defined order.
10. A survivor must belong to exactly one region roster at every tick boundary and must never be processed by two workers in the same tick.

Do not weaken these rules for convenience. See `docs/architecture/TECHNICAL-CLARIFICATIONS.md` for implementation detail.

## 7. Action, resource and intervention rules
- Workers evaluate actions; they do not directly apply mutations to shared state.
- Prefer an evaluation contract such as `ActionResult evaluate(ActionContext context)` over a method that mutates a shared `WorldContext` inside a worker.
- The result contains proposed deltas, claims, migration requests and events. TickCoordinator validates and applies it.
- Resource quantity must never become negative. Workers submit claims; the coordinator resolves them deterministically. `Resource` still protects its own invariant for any direct concurrent access and for the race-condition test.
- The Observer never directly mutates a Survivor. It proposes an `Intervention` command through the command queue.
- Observer analysis must be tied to a completed immutable snapshot and an explicit application tick. Thread scheduling must not decide when an intervention takes effect.
- Relationship changes across regions are returned as deltas and merged by TickCoordinator.

## 8. EventBus, Narrator and chapters
- Event objects are immutable and include event ID, tick, type, survivor IDs, location, severity, data and source/cause references.
- EventBus fans out to independent subscribers/queues. Narrator, Recorder, ObserverAI and Broadcaster must not compete for messages from one shared consumer queue.
- Narrator may filter, group and template real events, use verified character memories/relationships and keep a short anti-repetition history. It must never invent simulation facts.
- Every factual narrative entry should retain references to its source events.
- Use deterministic template selection. At least 40 authored variants are required for MUST.
- Chapter policies evaluate a read-only StoryState at a deterministic post-merge tick boundary. Chapter transitions are threshold-driven, not ordinary timer-driven.
- Use minimum dwell, pressure and a hard backstop. The hard backstop should not fire in the 20-seed chapter-arc test.
- Critical choices request a pause; the current tick must finish and merge before the simulation enters PAUSED. Apply the validated decision at a safe tick boundary.
- The MUST ending is implied and non-graphic. Keep templates restrained and include a banned-term test as specified by the blueprint.

## 9. Persistence rules
- PostgreSQL is the selected production database.
- Define persistence interfaces such as `EventRecorder` and `RunRepository` in the application/core boundary as appropriate.
- Provide JPA/PostgreSQL and in-memory adapters. Both must pass the same contract tests.
- Recording is asynchronous and must not block a normal tick.
- Critical records (decisions, interventions, chapter transitions and evidence) must not be intentionally dropped. Minor events may use a bounded queue and may be dropped/coalesced with metrics.
- If the critical queue reaches its configured high-water mark, request a safe pause rather than silently losing critical records or blocking a worker indefinitely.
- If PostgreSQL fails, switch to the in-memory fallback and clearly report that it is process-local and not crash-durable.
- Flush critical records during orderly shutdown.
- Do not persist every small survivor update on every tick. Persist important events, decisions, interventions, chapter changes, checkpoints and final state.
- Never commit passwords, tokens, connection strings containing credentials or local secret files. Use environment variables or ignored local configuration.

## 10. User experience and visual direction
The visual identity is Quiet Cinematic Minimalism. The Story page is primary. Avoid a conventional game HUD, persistent health bars, crowded dashboards, neon effects and unnecessary cards. Use charcoal `#171816`, ivory `#E8E3D9`, stone `#827C6D` and restrained rust `#79504A`. Recommended typography: Cormorant Garamond/Libre Baskerville for headings, Inter for body, IBM Plex Mono for Observer records. Use restrained scene imagery and slow transitions. The settlement map is optional and not shown by default.

## 11. Academic requirements
The implementation must visibly demonstrate:
- Encapsulation, abstraction, inheritance and polymorphism.
- All five SOLID principles with concrete class examples.
- Meaningful Strategy, Observer, Factory, Command, Template Method, Facade and State patterns.
- Producer-Consumer as a concurrency pattern (not a GoF pattern).
- Immutable snapshots as a data-sharing technique (not a GoF pattern).
- Four UML diagrams that match the implementation: class, sequence, state machine and component. Sequence coverage must include tick/ordered merge and a decision/pause flow. Add a cross-region handoff sequence if useful.

## 12. Required verification
For each task, run the narrowest relevant tests and report exact results. Required categories include:
- Unit tests for domain rules, behaviours, actions, crises and policies.
- Integration tests for engine, EventBus, Narrator, choices and persistence.
- Race-condition test on one shared Resource.
- Determinism test: same seed and decision log produce identical ordered events and final state hash, including a run with deliberately jittered worker timing.
- Region ownership and migration test.
- Concurrent command ordering test.
- Pause/resume boundary test.
- Shutdown test with no leaked workers or recorder thread.
- Chapter-arc test across 20 seeds; all MUST chapters occur in order and hard backstop is not used.
- Persistence adapter contract tests and slow-recorder test.
- Narrative integrity tests: captions cite source events, character references are valid, recap derives from stored history and Observer claims remain distinguishable from facts.

Do not place a deliberately failing unsafe race test in the normal test suite. Keep it as a separate controlled demonstration.

## 13. Claude working protocol - token-efficient
For every request:
1. Read this file and `PROGRESS.md` first. Read only the relevant blueprint sections; do not re-read the entire blueprint unless asked.
2. Inspect the current repository before changing anything. Do not assume files or prior work exist.
3. Work on exactly the requested milestone/task. Do not start future milestones or optional features.
4. Before editing, give a short plan and identify any ambiguity that blocks implementation.
5. Make the smallest complete change. Avoid speculative abstractions, duplicate files, unnecessary dependencies and unrelated refactoring.
6. Run relevant build/tests and report the exact commands and pass/fail results. Never claim a test passed unless it was run.
7. Update `PROGRESS.md` with completed work, tests, decisions, known issues and the next task.
8. Summarize changed files and any manual steps the user must perform. Keep explanations concise and suitable for a student who must defend the code in an AOOP viva.
9. Stop after the requested task. Wait for explicit approval before starting the next milestone.

Safety rules:
- Never delete or overwrite user files without explicit approval.
- Never run `git reset --hard`, force-push, destructive database commands or broad cleanup commands without explicit approval.
- Never install system-wide software or change Windows settings without explicit approval.
- Never expose secrets in terminal output, source files or commits.
- Do not commit or push unless the user explicitly asks.
- If a requirement conflicts with the blueprint, stop and explain the conflict rather than silently changing scope.
