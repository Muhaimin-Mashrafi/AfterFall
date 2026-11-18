# AfterFall: The Last Days of Humanity

A multithreaded, object-oriented interactive storytelling simulation of an AI-manipulated post-apocalyptic society. Built as a solo Advanced Object-Oriented Programming (AOOP) project.

## Concept

Approximately 20-30 autonomous survivors initially attempt to rebuild a settlement after civilization collapses. A rule-based AI known as the Observer secretly manipulates information and social conditions. The simulation generates real events; an event-driven Narrator turns important events into a documentary-style story. The user is the Humanity Overseer: they observe, inspect characters, make critical decisions and investigate recovered records.

The central moral is:

> The cruelest destruction is the kind that convinces people they chose it themselves.

## Current status

Pre-M0 setup. Application code has not yet been generated. See `PROGRESS.md` for the verified environment, milestone status and next steps.

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

## Build and run

Build/run commands will be added after M0 creates the Spring Boot application and Maven Wrapper. Do not guess commands before the project exists.

## Project governance

- `CLAUDE.md`: permanent architecture, scope, safety and coding instructions.
- `PROGRESS.md`: milestone tracker and session handoff.
- `docs/blueprint/`: canonical revised solo blueprint.
- `docs/architecture/`: approved engineering clarifications and decisions.
