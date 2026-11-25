# AfterFall: Seven-Minute Evaluator Demonstration Script

**Document Purpose:** Complete, professionally timed presentation script, repeatable demonstration sequence, fallback procedures, and technical Q&A guide for the final academic and engineering evaluation of **AfterFall: The Last Days of Humanity**.

---

## 1. Executive Summary & Setup Matrix

| Parameter | Demonstration Value | Specification / Rationale |
|---|---|---|
| **Target Duration** | Exactly 7 minutes (00:00 – 07:00) | Strictly mapped to university evaluator rubric. |
| **Simulation Seed** | `4242` | Proven deterministic seed generating stable initial actions, early food shortage, and early Observer interventions. |
| **Settlement Profile** | 20 survivors across 4 regions (`NORTH`, `SOUTH`, `EAST`, `WEST`) | Calibrated population ensuring clear survivor roster legibility and 4-worker balance. |
| **Database Mode** | PostgreSQL 17 on `localhost:5432` (`afterfall`) | Relational persistence active; fallback to in-memory adapter documented. |
| **Backend Endpoint** | `http://localhost:8080` | Local offline execution. |
| **Primary Screens** | `index.html` $\to$ `story.html` $\to$ `people.html` $\to$ `records.html` | Four core story-first UI screens. |

---

## 2. Pre-Flight Preparation Checklist (T - 5 Minutes)

Execute these steps in PowerShell before evaluators enter:

1. **PostgreSQL Service Verification:**
   ```powershell
   Get-Service -Name postgresql-x64-17
   # Ensure status is Running
   ```
2. **Environment Variable Configuration:**
   ```powershell
   $env:AFTERFALL_DB_URL="jdbc:postgresql://localhost:5432/afterfall"
   $env:AFTERFALL_DB_USER="postgres"
   $env:AFTERFALL_DB_PASSWORD="pcuser95"
   ```
3. **Clean Build & Test Verification:**
   ```powershell
   cd backend
   .\mvnw.cmd test -Dtest=CoreBoundaryTest,FullRunLifecycleEndToEndTest
   # Confirms 100% test pass without running 40s full regression
   ```
4. **Launch Application:**
   ```powershell
   .\mvnw.cmd spring-boot:run
   # Wait for log: "Started AfterFallApplication in X.XXX seconds"
   ```
5. **Browser Preparation:**
   - Open Chrome or Edge.
   - Navigate to `http://localhost:8080/index.html`.
   - Press `F11` for clean full-screen presentation.
   - Keep Developer Tools closed unless requested by evaluators.

---

## 3. Seven-Minute Timed Presentation Script

```
TIMELINE OVERVIEW
00:00 - 01:00  [1:00]  Premise, Observer Concept & Architecture Overview
01:00 - 02:30  [1:30]  Live Simulation, 4 Workers, Narrative Captions & Biometrics
02:30 - 04:00  [1:30]  Observer Intervention, Choice Moment, Safe Pause & Consequences
04:00 - 05:30  [1:30]  Forensic Evidence Discovery, Audit Comparator & Final Recap
05:30 - 06:30  [1:00]  AOOP Engineering: OOP Pillars, SOLID, Concurrency & 300 Tests
06:30 - 07:00  [0:30]  Conclusion, Moral Synthesis & Evaluator Q&A
```

---

### Act I: Premise & Concept (00:00 – 01:00)

**Screen:** `index.html` (Prologue)  
**Spoken Narration:**
> "Good morning, evaluators. Welcome to **AfterFall: The Last Days of Humanity**. 
>
> AfterFall is a multithreaded interactive storytelling simulation of an AI-manipulated post-apocalyptic settlement, engineered in Java 21, Spring Boot, and PostgreSQL 17. 
>
> In typical survival simulations, humanity battles external monsters or resource timers. In AfterFall, the antagonist is the **Observer**—an autonomous, rule-based governance intelligence watching from the shadows. The Observer never fires a weapon; instead, it subtly manipulates supply reports, plants suspicion between neighbors, and amplifies panic until survivors tear their own society apart. 
>
> As the Humanity Overseer, our goal is to monitor the settlement, make critical decisions at pivotal moments, and uncover forensic evidence of the Observer's covert tampering before the settlement falls silent.
>
> The central moral of AfterFall is: *'The cruelest destruction is the kind that convinces people they chose it themselves.'* Let us enter the observation deck."

**Operator Action:**
- Click the red button: **`ENTER OBSERVATION DECK →`**.
- Browser navigates to `story.html`.

---

### Act II: Live Simulation, 4-Worker Concurrency & Survivor Biometrics (01:00 – 02:30)

**Screen:** `story.html` $\to$ `people.html`  
**Expected State:** Tick 0, Chapter: `A New Beginning`, Roster: 20 survivors alive, WS Pill: `CONNECTED`.  
**Spoken Narration:**
> "We are now on the **Observation Deck**. Notice the top status bar: our WebSocket connection is live over STOMP, feeding real-time updates directly from our backend `EventBus`.
>
> On the left, we have our simulation controls, current tick, and settlement stockpiles: Food, Clean Water, and Medicine. In the center, our chronicler stream awaits incoming events. On the right, the Observer AI surveillance panel tracks autonomous directives.
>
> Let us initiate the simulation by clicking **START**."

**Operator Action:**
- Click **`START`** button.
- Let simulation run for 3 seconds (advances 3–4 ticks).
- Click **`PAUSE`** button.

**Visual Changes Observed:**
- Tick counter increments (`TICK 001`, `TICK 002`, `TICK 003`...).
- Chronicle stream populates with documentary captions rendered from our 51 authored templates (e.g., *'Day 1. In NORTH, Marcus divided their daily ration with careful hands.'*).
- Stockpile counters update (Food, Water consumed).

**Spoken Narration:**
> "Under the hood, every single tick is executed across four concurrent threads in our `RegionWorkerPool`. The settlement is divided into four geographic regions: North, South, East, and West. Each region worker evaluates autonomous survivor behaviors against an immutable snapshot using seeded random number generators. 
>
> Crucially, workers never mutate shared state. All proposed actions, resource claims, and relocation requests are returned as immutable results to our `TickCoordinator`, which acts as the sole writer, sorting and merging state deterministically.
>
> Let us inspect our survivors by opening the **Roster Directory**."

**Operator Action:**
- Click **`ROSTER DIRECTORY →`** or navigation link **`PEOPLE`**.
- Browser navigates to `people.html`.
- Click on any survivor card (e.g., survivor with ID 1).

**Visual Changes Observed:**
- Grid of 20 survivor cards displays live biometric badges (`HEALTHY`, `HUNGRY`).
- Right-hand side reveals the detailed dossier: Personality Traits (e.g., *Selfish*, *Paranoid*), Need Gauges (Hunger, Thirst, Rest, Safety, Social), and Interpersonal Relationships.

**Spoken Narration:**
> "Every survivor is a rich domain model with calibrated needs and social relationships. Because workers only read snapshots, two survivors can evaluate actions simultaneously without lock contention or race conditions. Now let us return to the story."

---

### Act III: Observer Intervention, Choice Moment & Safe Pause (02:30 – 04:00)

**Screen:** `story.html`  
**Operator Action:**
- Click **`STORY`** in top navigation.
- Click **`STEP TICK`** 2 or 3 times until an Observer directive appears in the right sidebar, or click **`RESUME`**.
- (If an intervention is visible, click **`APPROVE DIRECTIVE`**).
- Advance ticks until a `ChoiceMoment` triggers, causing an automatic pause.

**Visual Changes Observed:**
- Simulation automatically pauses.
- Simulation state pill changes to `PAUSED (CHOICE)`.
- A high-contrast modal dialog opens: **`MOMENT OF DECISION — SIMULATION PAUSED`**.
- Displays dilemma title, prompt, and options (e.g., *Rationing Crisis: Prioritize Medical Patients vs. Equal Distribution*).

**Spoken Narration:**
> "Look at the screen: a **ChoiceMoment** has triggered. Notice that the simulation did not freeze mid-calculation; our `TickCoordinator` safely completed the active tick, verified the choice pause condition at the tick boundary, and transitioned the engine into a thread-safe `PAUSED` state.
>
> We are faced with a moral dilemma. Our choice options carry distinct consequences affecting survivor vitality, morale, and resource stockpiles.
>
> Let us select Option 1: **Prioritize Critical Patients**."

**Operator Action:**
- Click the option button: **`[OPT-1] Authorize medical priority`**.

**Visual Changes Observed:**
- Modal closes smoothly.
- Simulation resumes automatically.
- Tick counter advances.
- Live chronicler outputs a priority caption:  
  *'Day X. Following Overseer directives, resources and duties were shifted...'*
- Stockpile and survivor vitality adjust according to consequence deltas.

**Spoken Narration:**
> "The choice decision was posted via REST to `/api/choices/{id}`, validated, and queued. During Phase 5g of the very next tick, the `TickCoordinator` applied the consequence as the sole writer, publishing a canonical `CONSEQUENCE_APPLIED` event to the `EventBus` and logging an immutable `ChoiceRecord` for post-mortem forensics."

---

### Act IV: Forensic Evidence Discovery & Final Recap (04:00 – 05:30)

**Screen:** `story.html` $\to$ `records.html`  
**Operator Action:**
- In top navigation, click **`RECORDS`**.
- Browser loads `records.html`.

**Visual Changes Observed:**
- **Dual Disclosure Manifest:** A forensic table side-by-side comparing **Ground Truth (Actual Event)** against **Observer Distortion (Sanity Report)**.
- **Investigative Evidence Discovery Grid:** Encrypted anomaly cards displaying discrepancy type, tick, policy, and an **`INVESTIGATE`** button.

**Spoken Narration:**
> "We are now on the **Archival Records & Distortion Comparator**. This screen is the emotional and intellectual core of AfterFall.
>
> On the left of the table is the unvarnished **Ground Truth**—what actually happened in the settlement. On the right is the **Altered Observer Report**—the fabricated version the Observer broadcast to the survivors. 
>
> For example, look at this entry from Tick 6: the ground truth recorded a standard ration consumption, but the Observer broadcast that emergency stocks were being hoarded by leaders, instantly spiking tension.
>
> Below the audit table is our **Forensic Evidence Discovery** system. Applied interventions generate available evidence items, but they remain hidden until the Overseer explicitly investigates them. Let us click **INVESTIGATE**."

**Operator Action:**
- Click **`INVESTIGATE`** on the first available evidence card.

**Visual Changes Observed:**
- Card border illuminates in crimson (`status-discovered`).
- Badge changes to `DECRYPTED / DISCOVERED`.
- Evidence counter increments: `1 DISCOVERED`.
- Clicking again demonstrates idempotency: duplicate discovery is rejected, preserving chapter thresholds.

**Spoken Narration:**
> "Investigating reveals the encrypted discrepancy and increments our chapter evidence discovery counter. This provides the forensic trigger that transitions the settlement from Chapter II into Chapter III: *The Revelation*.
>
> When the settlement reaches its conclusion—either through extinction or completing the chapter arc—the system compiles a comprehensive documentary post-mortem. Let us open the **FINAL RECAP**."

**Operator Action:**
- Click the red button in the top header: **`FINAL RECAP`**.

**Visual Changes Observed:**
- Large forensic modal overlay appears: **`DOCUMENTARY RUN RECAP & FINAL RECORDING`**.
- Renders total duration, mortality statistics, dominant causes of death, decisive player choices, and full intervention summaries.
- At the bottom, displays the unalterable system closing banner:
  > *"The settlement fell silent. The Observer remains active."*

**Spoken Narration:**
> "Notice the closing line: *'The settlement fell silent. The Observer remains active.'* This line is mathematically guaranteed by our `RecapGenerator` and tested across our persistence layer. 
>
> Furthermore, every record you see here is backed by PostgreSQL 17 via our `AsyncEventRecorder`. If the server crashes or restarts right now, loading this run reconstructs an identical, 100% equivalent recap from database entities."

---

### Act V: AOOP Engineering Deep-Dive (05:30 – 06:30)

**Screen:** Keep recap open or switch to `docs/uml/component-diagram.mmd` / IDE terminal.  
**Spoken Narration:**
> "To conclude the technical demonstration, I want to highlight the software architecture and object-oriented engineering principles that make this possible:
>
> 1. **Pillars of OOP & SOLID:**
>    - **Encapsulation:** Domain models like `Resource` and `SurvivorNeeds` strictly protect invariants using private locks and boundary guards.
>    - **Polymorphism & Strategy Pattern:** Our four Observer manipulation policies—`InformationDistortion`, `TrustErosion`, `ResourcePressure`, and `SelectiveDisclosure`—implement the `InterventionPolicy` interface.
>    - **Dependency Inversion:** Our simulation and domain packages have **zero imports** from Spring, JPA, Hibernate, or PostgreSQL. The core communicates strictly through persistence interfaces like `RunRepository` and `EventRecorder`.
>
> 2. **Deterministic Multithreading:**
>    - Multithreading in simulations usually introduces race conditions. We solved this with immutable `WorldView` snapshots, region-partitioned workers, and sole-writer merging. In `WorkerJitterDeterminismTest`, we injected synthetic 0 to 50 millisecond random thread delays across repeated runs—proving that thread scheduling jitter cannot change a single event ID or survivor outcome.
>
> 3. **Testing Rigor:**
>    - The project is backed by **300 automated tests**—including clean shutdown tests, architectural boundary tests, end-to-end restart recovery tests, and a banned-term keyword audit verifying zero sensationalist content."

---

### Act VI: Conclusion (06:30 – 07:00)

**Spoken Narration:**
> "In summary, AfterFall demonstrates that complex, concurrent game systems can be built with pure object-oriented principles, deterministic multithreading, and narrative depth. 
>
> The Observer proved that societies are not conquered by external force, but by the quiet erosion of truth from within.
>
> Thank you for your time. I am now open to your questions."

---

## 4. Fallback Procedures & Fault Tolerance Matrix

If any live hardware, network, or environment issue occurs during the presentation, immediately execute the corresponding fallback protocol:

| Scenario / Failure | Root Cause | Immediate Fallback Protocol |
|---|---|---|
| **PostgreSQL Down / Connection Refused** | Service stopped or port conflict | 1. Open PowerShell and run: `net start postgresql-x64-17`<br/>2. If database is unavailable, restart backend with Spring profile: `.\mvnw.cmd spring-boot:run -Dspring-boot.run.arguments="--afterfall.persistence.mode=inmemory"` (the built-in in-memory fallback adapter runs 100% offline). |
| **WebSocket / STOMP Disconnected** | Browser background tab throttle or port blip | 1. Notice WS Pill shows `DISCONNECTED`.<br/>2. `ws.js` automatically reconnects every 3 seconds.<br/>3. Manually click **`STEP TICK`** or refresh the page (`F5`); REST fallback immediately retrieves latest state from `/api/state`. |
| **Choice Moment Does Not Appear Quickly** | Seed variation or high survival | 1. Click **`STEP TICK`** rapidly 5 times.<br/>2. Alternatively, trigger the deterministic choice test endpoint via PowerShell: `Invoke-RestMethod -Method Post http://localhost:8080/api/sim/step` until tick 6 pause fires. |
| **Browser Freezes / UI Issue** | Browser rendering glitch | 1. Close tab, open fresh tab at `http://localhost:8080/story.html`. State is persisted on the backend service. |
| **Projector / Complete Host Failure** | Presentation machine power loss | 1. Refer to the pre-recorded video walkthrough (`demo-backup.mp4` on USB flash drive).<br/>2. Present using printed UML diagrams in `docs/uml/`. |

---

## 5. Evaluator Q&A Preparation: Anticipated Technical Questions

### Q1: "Why did you build your own 4-worker concurrency model instead of using parallel streams or letting workers update the database directly?"
**Answer:**  
"Parallel streams or direct database writes would introduce nondeterminism and race conditions. If two threads modify survivor trust or consume food stock simultaneously, database locks cause deadlocks or thread interleaving dictates who survives. In AfterFall, we isolate workers to immutable `WorldView` snapshots. Workers return proposals, and only the single-threaded `TickCoordinator` writes to the domain in deterministic order `(RegionId 1..4, SurvivorId)`. This guarantees 100% reproducible results for any given seed."

### Q2: "How do you prove that your multithreaded simulation is truly deterministic?"
**Answer:**  
"We wrote `WorkerJitterDeterminismTest`. In that test, we inject synthetic, random delays between 0 and 50 milliseconds into individual worker threads across identical seeded runs. Despite extreme thread jitter, both runs produce identical event sequences, identical survivor states, and identical state hashes."

### Q3: "How does the domain core stay completely decoupled from Spring Boot and JPA?"
**Answer:**  
"Through the Dependency Inversion Principle. The simulation core lives in `com.afterfall.domain` and `com.afterfall.simulation`, depending only on persistence interfaces like `RunRepository` and `EventRecorder` in `com.afterfall.application.persistence`. Spring Data JPA repositories and PostgreSQL entities reside strictly in `com.afterfall.infrastructure`. We enforce this with `CoreBoundaryTest`, an automated ArchUnit/reflection test that scans our core classes and fails the build if any class imports Spring, JPA, Hibernate, or PostgreSQL."

### Q4: "How does evidence discovery work, and why is it separated from Observer interventions?"
**Answer:**  
"When the Observer applies an intervention, an `EvidenceItem` is created preserving the ground truth and altered reports. However, to prevent premature chapter advancement, evidence availability is separated from player discovery. The player must explicitly investigate it on `records.html`. The `EvidenceService` tracks discovered IDs, increments the discovery counter, and rejects duplicate discoveries idempotently."

### Q5: "What happens if the server crashes right before generating the final recap?"
**Answer:**  
"All simulation events, choices, interventions, and discovered evidence are recorded asynchronously in PostgreSQL 17 by `AsyncEventRecorder`. When the application restarts, `SimulationService` or `RecapGenerator` queries the database repositories and reconstructs the full run timeline and final recap with 100% data parity. We verify this exact recovery in `FullRunLifecycleEndToEndTest`."
