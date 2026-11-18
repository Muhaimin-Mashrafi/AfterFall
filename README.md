# AfterFall: The Last Days of Humanity

A multithreaded, object-oriented interactive storytelling simulation of an AI-manipulated post-apocalyptic society. Built as a solo Advanced Object-Oriented Programming (AOOP) project.

## Concept

Approximately 20-30 autonomous survivors initially attempt to rebuild a settlement after civilization collapses. A rule-based AI known as the Observer secretly manipulates information and social conditions. The simulation generates real events; an event-driven Narrator turns important events into a documentary-style story. The user is the Humanity Overseer: they observe, inspect characters, make critical decisions and investigate recovered records.

The central moral is:

> The cruelest destruction is the kind that convinces people they chose it themselves.

## Current status

M0 complete: Spring Boot backend skeleton, package boundaries and initial component diagram. No simulation logic, persistence, or frontend yet. See `PROGRESS.md` for milestone status and next steps.

## Canonical project specification

Read `docs/blueprint/AfterFall-Blueprint-Revised-Solo.md`. It is the authoritative solo-scope blueprint. `CLAUDE.md` contains the coding and architecture rules for contributors and Claude Code.

## Technology direction

- Java 21
- Spring Boot
- PostgreSQL 17
- Maven Wrapper
- HTML, CSS and JavaScript
- REST and WebSocket/STOMP
- JUnit 5 and Mockito

Spring Boot is required. PostgreSQL is selected. The simulation core must remain plain Java and independent of Spring and JPA.

## MUST scope

- 20-30 configurable survivors.
- Four processing regions and four region workers.
- Deterministic tick coordination and ordered merge.
- Food, water and medicine.
- Famine and Disease Outbreak.
- Four deterministic Observer policies.
- EventBus with independent subscriber fan-out.
- Narrator with at least 40 authored templates.
- Four threshold-driven chapters.
- Critical choices with safe pause/resume.
- PostgreSQL behind persistence interfaces and an in-memory adapter.
- Asynchronous recording.
- Story-first interface: Opening, Story, People, Recovered Records and Final Recording.
- Metrics, UML, documentation and automated tests.

Audience voting, a map, 100-survivor scale-up, extra crises, factions, trading, replay, sound and external LLM narration are optional. Do not let optional work delay MUST completion.

## Local development prerequisites

- Windows 10/11 x64 (current development environment: Windows 10 Pro x64).
- JDK 21.
- Git for Windows.
- PostgreSQL 17 with a local database named `afterfall`.
- VS Code with Extension Pack for Java and Spring Boot Extension Pack.
- Claude Code may be used only with an individually authorized account.

Never commit passwords, API keys, access tokens or local secret configuration. Use environment variables or ignored local configuration.

## Repository layout

```
backend/                 Spring Boot 4.1.2 Maven project (Java 21, jar packaging)
  mvnw, mvnw.cmd, .mvn/  Maven Wrapper (downloads Maven 3.10.1; no global Maven needed)
  src/main/java/com/afterfall/
    AfterFallApplication.java   Spring Boot entry point
    domain/          plain Java domain model (no Spring/JPA imports)
    simulation/      plain Java simulation core (no Spring/JPA imports)
    application/     use cases and ports (interfaces) such as EventRecorder
    infrastructure/  adapters (persistence, messaging)
    web/             REST and WebSocket/STOMP adapters
    config/          Spring bean wiring
docs/uml/component-diagram.mmd  initial Mermaid component diagram
```

`CoreBoundaryTest` fails the build if `domain` or `simulation` sources import Spring, JPA, Hibernate or PostgreSQL classes.

## Build and run

Run from the `backend` directory in PowerShell (JDK 21 must be on `PATH` / `JAVA_HOME`):

```powershell
cd backend
java -version                 # expect 21.x
.\mvnw.cmd test              # context-load test + core boundary test
.\mvnw.cmd package           # builds target\afterfall-backend-0.0.1-SNAPSHOT.jar
java -jar target\afterfall-backend-0.0.1-SNAPSHOT.jar   # starts on http://localhost:8080
```

Alternatively, `.\mvnw.cmd spring-boot:run`. At M0 there are no endpoints yet, so `http://localhost:8080/` returns 404; this is expected. Stop the app with Ctrl+C. No database is required until M9.

## Project governance

- `CLAUDE.md`: permanent architecture, scope, safety and coding instructions.
- `PROGRESS.md`: milestone tracker and session handoff.
- `docs/blueprint/`: canonical revised solo blueprint.
- `docs/architecture/`: approved engineering clarifications and decisions.
