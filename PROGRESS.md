# AfterFall Development Progress

This file is the handoff point between Claude Code sessions. Update it after every completed task. Keep it factual and concise.

## Current status
- Project: AfterFall - The Last Days of Humanity
- Canonical specification: `docs/blueprint/AfterFall-Blueprint-Revised-Solo.md`
- Current milestone: Environment setup / pre-M0
- Application code: Not started
- Last updated: 2026-11-18

## Environment (user-reported)
- Windows 10 Pro, 64-bit.
- Eclipse Temurin JDK 21.0.13 installed; `java`, `javac` and `JAVA_HOME` verified.
- VS Code 1.146.0 installed.
- Git 2.45.1 installed; Git Bash exists at `C:\Program Files\Git\bin\bash.exe`.
- Node.js 26.13.1 and npm 11.20.0 installed.
- PostgreSQL 17.12 installed; Windows service `postgresql-x64-17` was running.
- `psql` connection to `localhost:5432` as `postgres` succeeded.
- pgAdmin 4 opens; database `afterfall` was created and visible in pgAdmin.
- AfterFall workspace: `C:\Users\user\Projects\AfterFall`.
- Java Extension Pack appears installed; VS Code status bar showed `Java: Ready`.
- Spring Boot Extension Pack installation was observed in progress; verify it is fully installed before M0.
- Claude Code VS Code extension appears installed. Authentication is not recorded as complete. Do not authenticate using a shared individual Pro login; use an individually authorized account.

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
| M0 | Spring Boot project setup, packages, initial UML | Not started |
| M1 | Domain model and OOP pillars | Not started |
| M2 | Behaviours, actions and core patterns | Not started |
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
- Verify Spring Boot Extension Pack finished installing.
- Verify `git status`, current branch and `origin` remote.
- Confirm Claude Code can be used with an individually authorized account; do not use a shared Pro login.
- Maven is the recommended build tool; use Maven Wrapper. Confirm only if the course imposes another build tool.

## Latest completed task
- Environment and GitHub setup were performed by the user. The user reports the Git commands and remote setup are complete; command output has not yet been reviewed in this conversation.

## Next task
1. Add this starter pack to the local workspace.
2. Verify Git status and remote.
3. Verify Spring Boot extensions and authorized Claude Code access.
4. Commit the project instructions and canonical blueprint.
5. Start M0 only after the user approves the M0 prompt.

## Session log
- 2026-11-18: Environment setup guided; JDK 21 and PostgreSQL 17.12 verified; `afterfall` database created; VS Code workspace and Java extension pack prepared; private GitHub repository created; revised solo blueprint adopted as source of truth.
