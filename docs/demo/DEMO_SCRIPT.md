# AfterFall: Seven-Minute Evaluator Demonstration Script

**Document Purpose:** Complete, professionally timed presentation script, repeatable demonstration sequence, fallback procedures, and technical Q&A guide for the final academic and engineering evaluation of **AfterFall: The Last Days of Humanity**.

---

## 1. Setup Matrix

| Parameter | Value | Notes |
|---|---|---|
| **Target duration** | 7 minutes (00:00 to 07:00) | Rehearsal pass 2 must land between 6:30 and 7:00. |
| **Run seed** | `42` (built-in default) | The same seed gives the same run every time; checked twice on the packaged app. |
| **Settlement** | 25 survivors, 4 regions | Roster page shows 25 cards. |
| **Database mode** | PostgreSQL 17 `afterfall` on `localhost:5432` | Offline fallback: `inmemory` profile (section 4). |
| **Backend** | `http://localhost:8080` | Local only. |
| **Screens** | `index.html`, `story.html`, `people.html`, `records.html` | Final recap opens as a modal on `records.html`. |

### 1.1 How time works in this build (read this first)
- **Nothing advances the simulation by itself.** `START` and `RESUME` only change the lifecycle pill (`PAUSED`/`RUNNING`). The tick counter moves only when `STEP TICK` is clicked (or `POST /api/sim/step` is called). Checked: 5 seconds after `START` the counter was still 0.
- The arc is long: Chapter IV starts at about tick 179. Nobody can click that live in seven minutes, so the demo **stages the run**: a few live clicks for the audience, then a PowerShell fast-forward helper (`Step-To`, section 2) to the next story beat, then one or two live clicks to land on it. Say so out loud ("let us skip ahead to day 12"); do not hide it.
- A fast-forward does not redraw the tick counter or chapter label. Click `STEP TICK` once afterwards (or press `F5`) and the page catches up.
- Speed: about 60 clicks took 4 seconds in a scripted browser, so `Step-To` over 40 ticks takes a second or two. A human clicking by hand manages roughly one tick per second.

### 1.2 Run map (default seed 42, choices left on the first option)
"Counter" is what the TICK field on the Story page shows after the step that triggers the event. Event ticks in the Records table and captions are one lower.

| Counter | What appears | Where |
|---|---|---|
| 0 to 8 | Quiet opening: meals, foraging, daily routine captions (about 12 captions per tick). Food: 50 at the start, 53 at counter 8, 57 around tick 20, 39 around tick 40. | Story |
| 47 | **Chapter II: The Fracture** (label changes, Observer intervention applied; first `RESOURCE_PRESSURE` record at tick 46). | Story, Records |
| 57 | **Famine begins; choice "The Last Stores" pauses the run** (pill `PAUSED`). Food is 9, then reads 0 from about tick 60. | Story |
| 67 | **Chapter III: The Revelation** (Observer exposure reached 60). | Story |
| 98 to 130 | Many medical-recovery captions (illness spreading, medicine falls from 20 to about 8 by tick 120 and is 0 by tick 135). | Story |
| 135 | **Disease Outbreak begins; choice "The Sick Quarter" pauses the run.** | Story |
| about 167 | First deaths ("Another voice fell silent..."). | Story |
| 179 | **Chapter IV: The Last Recording** (population fell below 20). 15 alive, 10 dead at 180. | Story |
| 200 / 220 | 8 alive / 4 alive. | Story, People |
| 280 / 400 | 2 survivors remain (stable to tick 400). | Recap |

Numbers were measured by stepping the packaged app (`inmemory` profile) through 420 ticks, answering both choices with option 1 ("Share the stores equally", "Separate the sick"). Other options change food, trust and safety, so later numbers can shift slightly; do the final rehearsal with the option you will pick.

### 1.3 Seven-minute budget

| Time | Act | Live clicks | Fast-forward | Counter at end |
|---|---|---|---|---|
| 00:00 to 01:00 | I Premise | none | none | 0 |
| 01:00 to 02:30 | II Live simulation, workers, roster | 8 `STEP TICK` | none | 8 |
| 02:30 to 04:00 | III Chapter II, famine choice, Chapter III | 3 + 2 + 2 clicks | `Step-To 44`, `Step-To 55`, `Step-To 65` | 67 |
| 04:00 to 05:30 | IV Records, outbreak, Chapter IV, recap | 2 + 4 + 1 clicks | `Step-To 133`, `Step-To 175`, `Step-To 219` | 220 |
| 05:30 to 06:30 | V Engineering | none | none | 220 |
| 06:30 to 07:00 | VI Conclusion | none | none | 220 |

---

## 2. Pre-Flight Preparation Checklist (T minus 5 minutes)

Run in PowerShell before the evaluators enter:

1. **PostgreSQL service**
   ```powershell
   Get-Service -Name postgresql-x64-17
   # Status must be Running
   ```
2. **Environment variables** (the real password is typed only in your own shell; never save it in a file or leave it on screen)
   ```powershell
   $env:AFTERFALL_DB_URL="jdbc:postgresql://localhost:5432/afterfall"
   $env:AFTERFALL_DB_USER="postgres"
   $env:AFTERFALL_DB_PASSWORD="<your_postgres_password>"
   ```
3. **Quick test check**
   ```powershell
   cd backend
   .\mvnw.cmd test -Dtest=CoreBoundaryTest,FullRunLifecycleEndToEndTest
   ```
4. **Launch (always a fresh process: the run starts at tick 0 only once)**
   ```powershell
   .\mvnw.cmd spring-boot:run
   # wait for: "Started AfterFallApplication in X.XXX seconds"
   ```
5. **Paste the helpers into a second PowerShell window** and keep it beside the browser:
   ```powershell
   $api = "http://localhost:8080/api"
   function Step-To($t) {
     while ((Invoke-RestMethod "$api/state").tick -lt $t) {
       Invoke-RestMethod -Method Post "$api/sim/step" | Out-Null
     }
   }
   function Answer($id, $opt = 0) {
     Invoke-RestMethod -Method Post "$api/choices/$id" -ContentType 'application/json' `
       -Body (@{ optionIndex = $opt } | ConvertTo-Json) | Out-Null
   }
   ```
   Type `Step-To 44` and `Answer CHOICE_FAMINE 0` only when the script says so. The equivalent `curl` calls were tested on the packaged app; the PowerShell wrappers were not run on Windows, so test them once during rehearsal pass 1, then restart the app.
6. **Browser:** Chrome or Edge, `http://localhost:8080/index.html`, F11, zoom 100 percent, DevTools closed.
7. **Do not click anything on the Story page before the script says so.** If the run is already past tick 0, restart the app.

---

## 3. Seven-Minute Timed Presentation Script

```
TIMELINE
00:00 - 01:00  Premise, Observer concept
01:00 - 02:30  Live ticks, 4 workers, captions, roster       (counter 0 -> 8)
02:30 - 04:00  Chapter II, famine choice, Chapter III        (counter 8 -> 67)
04:00 - 05:30  Records, outbreak, deaths, Chapter IV, recap  (counter 67 -> 220)
05:30 - 06:30  AOOP engineering
06:30 - 07:00  Conclusion and questions
```

> If PR 5 (cinematic frontend) is merged: chapter changes also show a full-screen title card (click or Esc to dismiss, add about 3 seconds per chapter), crisis declarations show a red band, and the final recording is a staged full-screen sequence ending on the closing line, with "Open full recap" for the modal. This script was run against the frontend without PR 5; re-time one pass after merging.

---

### Act I: Premise and Concept (00:00 to 01:00)

**Screen:** `index.html`
**Spoken narration:**
> "Good morning, evaluators. This is **AfterFall: The Last Days of Humanity**, a multithreaded, interactive storytelling simulation of an AI-manipulated post-apocalyptic settlement, built in Java 21, Spring Boot and PostgreSQL 17.
>
> The antagonist is not a monster. It is the **Observer**, a deterministic, rule-based intelligence that never fires a weapon. It bends supply reports, plants suspicion between neighbours and lets survivors tear their own society apart.
>
> I am the Humanity Overseer. I watch, I make hard decisions at pivotal moments, and I can investigate the Observer's records. The survivors act on their own.
>
> The moral of AfterFall is: *'The cruelest destruction is the kind that convinces people they chose it themselves.'*"

**Operator action:** click the red button **`[ INITIATE OBSERVATION PROTOCOL ]`**. The browser opens `story.html`.

---

### Act II: Live Simulation, 4 Workers, Roster (01:00 to 02:30)

**Screen:** `story.html`, then `people.html`
**Expected state:** counter 0, pill `PAUSED`, chapter `CHAPTER_1_A_NEW_BEGINNING`, stream pill `STREAM LIVE`, 25 alive, Food 50, Water 50, Medicine 20.
**Spoken narration:**
> "This is the observation deck. The stream pill shows our STOMP connection to the backend EventBus. On the left are the controls and the three stockpiles, in the centre the chronicle, on the right the Observer panel. Let me start the run."

**Operator action:**
1. Click **`START`**. The pill turns `RUNNING`. The counter stays at 0. Say: "The engine is armed; I advance it one tick at a time, so every step is reproducible."
2. Click **`STEP TICK`** 8 times, about one per second, narrating the captions as they arrive.

**Visual changes:** counter 1 to 8, about a dozen captions per tick (meals, foraging, quiet routines), Food around 51 to 57.

**Spoken narration (during the clicks):**
> "Every tick runs on four worker threads, one per region: North, South, East and West. Each worker reads an immutable snapshot of the world and returns an immutable result. Workers never write shared state. The `TickCoordinator` is the only writer; it merges region by region, survivor by survivor, in a fixed order, so thread timing cannot change the outcome."

**Operator action:** click **`ROSTER DIRECTORY`**, then click any survivor card.

**Visual changes:** 25 cards with health, hunger and thirst readouts and a dossier on the right (traits, needs, relationships).

**Spoken narration:**
> "Each survivor is composed from needs, health, personality, inventory and a profession. Because workers only read snapshots, survivors are evaluated in parallel without lock contention. Back to the story."

Click **`STORY`**.

---

### Act III: Chapter II, the Famine Choice, Chapter III (02:30 to 04:00)

**Screen:** `story.html`

**Step 1: Chapter II (about 30 seconds).**
Spoken: "The first weeks pass quietly. Let us skip ahead to day 11." In the second window run `Step-To 44`. Click **`STEP TICK`** 3 times (counter 45, 46, 47).

Visual: at counter 47 the chapter label changes to `CHAPTER_2_THE_FRACTURE`; Food is down to about 28; the Observer panel shows its first intervention.
Spoken: "Chapter II, The Fracture. This chapter change was not on a timer: it fired because an Observer intervention produced visible discord. The Observer saw the reserves drop and applied a resource-pressure policy."

**Step 2: The famine choice (about 60 seconds).**
Spoken: "Let us skip to the famine." Run `Step-To 55`. Click **`STEP TICK`** twice (counter 57).

Visual: the pill changes to `PAUSED`, Food is 9 and falls to 0 within three ticks. **No dialog opens** (see section 4, "Choice dialog does not open"). The captions include "Day 15. Despair gathered quietly around the dried grain bins."

Spoken:
> "A critical choice has paused the run. The tick that raised it finished and merged first; only then did the engine enter `PAUSED`. The choice is 'The Last Stores': the storehouse is nearly bare. Share the stores equally, feed the workers first, or follow the Observer's advisory. I will share equally."

Operator action: in the second window run `Answer CHOICE_FAMINE 0`. Click **`RESUME`**, then **`STEP TICK`** once, then press `F5` so the consequence caption is visible (if you cannot see it, scroll the chronicle to tick 57).

Visual: caption "Day 15. Following Overseer directives, resources and duties were shifted: Equal rations were ordered, and the stores stretched a little further."
Spoken:
> "The decision was validated, queued, and applied at the tick boundary by the coordinator as the sole writer; it is recorded as an immutable choice record. Food stays at zero or one from here on: survivors live on what they forage, about one ration per three ticks."

**Step 3: Chapter III (about 20 seconds).**
Run `Step-To 65`. Click **`STEP TICK`** twice (counter 67).
Visual: label `CHAPTER_3_THE_REVELATION`.
Spoken: "Chapter III, The Revelation. The trigger was Observer exposure reaching level 60: the more the Observer manipulates, the closer the truth gets to the surface."

---

### Act IV: Records, the Outbreak, the Last Chapter, Final Recap (04:00 to 05:30)

**Screen:** `records.html`, then `story.html`, then `records.html`

**Step 1: Records (about 40 seconds).** Click **`RECORDS`** in the navigation. Header shows about 6 records catalogued.
Visual: a table of ground truth against Observer report. The first row is tick 46, Region 1: ground truth "Current reserves: Food=30, Meds=20. Consumption steady." against "EMERGENCY ADVISORY: Stockpiles nearing irreversible depletion. Immediate rationing advised." Further rows at ticks 50, 54, 58, 62, 66. All show `APPLIED`.
Spoken:
> "This is the Archive and Distortion Comparator. Left is what actually happened. Right is what the Observer told the survivors. At tick 46 the stores were fine and the Observer said they were nearly gone. Applied interventions produce evidence that stays hidden until I investigate it."

Click **`INVESTIGATE`** on the first card. Visual: the card turns decrypted and the counter reads `1 DISCOVERED`. Clicking the same item again does nothing (duplicate discovery is rejected).

**Step 2: Outbreak and deaths (about 30 seconds).** Click **`STORY`**.
Spoken: "Let us return to the settlement and skip ahead about two weeks." Run `Step-To 133`. Click **`STEP TICK`** twice (counter 135).
Visual: the pill goes `PAUSED` again, the Outbreak choice is pending, and captions mention fever and recovery. No dialog opens.
Spoken: "A second crisis, Disease Outbreak, raised a second choice, 'The Sick Quarter'. I will separate the sick."
Run `Answer CHOICE_OUTBREAK 0`, click **`RESUME`**.

Run `Step-To 175`, click **`STEP TICK`** 4 times (counter 179).
Visual: deaths in the stream (e.g. "Another voice fell silent in the bitter chill of Quadrant 2."), the label becomes `CHAPTER_4_THE_LAST_RECORDING`, the left panel shows about 18 alive and 7 casualties at counter 179, falling to 15 and 10 a tick later.
Spoken: "Chapter IV, The Last Recording. The trigger: population fell below 20. Deaths came to the weakest first, driven by hunger and illness, and the Observer's selective-disclosure policy kept running."

Run `Step-To 219` and click **`STEP TICK`** once. Counter 220, 4 alive, 21 casualties.

**Step 3: Final recap (about 20 seconds).** Click **`RECORDS`**, then **`FINAL RECAP`**.
Visual: "DOCUMENTARY RUN RECAP & FINAL RECORDING" with tiles TOTAL TICKS 220, SURVIVORS ALIVE 4 / 25, CASUALTIES 21, EVIDENCE DECODED 1 and the final stockpile (FOOD 0, WATER 50, MEDICINE 0).
Spoken: "The recap is built from the recorded history, not from live memory."

**Do not scroll through the scene list.** It runs to about 70 lines, 66 of them "Day N. Observer record: resource pressure concerning ..." (Region 1 or a survivor number), and contains no deaths or character names (see section 6). Press `End` or drag to the bottom and read the closing line:
> *"The settlement fell silent. The Observer remains active."*

Spoken: "That closing line is guaranteed by `RecapGenerator` and covered by tests. Every record behind this recap is persisted asynchronously in PostgreSQL by `AsyncEventRecorder`."

---

### Act V: AOOP Engineering Deep-Dive (05:30 to 06:30)

**Screen:** Close the recap, then show the rendered UML in `docs/uml/rendered/` or the IDE terminal.  
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
>    - The project is backed by **about 310 automated tests** (quote the exact number from your last green `mvnw.cmd test`; 310 at the time of writing, with PostgreSQL 17 available)—including clean shutdown tests, architectural boundary tests, end-to-end restart recovery tests, and a banned-term keyword audit verifying zero sensationalist content."

---

### Act VI: Conclusion (06:30 to 07:00)

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
| **PostgreSQL Down / Connection Refused** | Service stopped or port conflict | 1. Open PowerShell and run: `net start postgresql-x64-17`<br/>2. If database is unavailable, restart the backend in offline mode: `java -jar target\afterfall-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=inmemory` (the in-memory adapter runs fully offline; it is process-local and not crash-durable). The run is identical, so the script is unchanged. Re-paste the helpers if you opened a new window. |
| **WebSocket / STOMP Disconnected** | Browser background tab throttle or port blip | 1. Notice WS Pill shows `DISCONNECTED`.<br/>2. `ws.js` automatically reconnects every 3 seconds.<br/>3. Manually click **`STEP TICK`** or refresh the page (`F5`); REST fallback immediately retrieves latest state from `/api/state`. |
| **Choice dialog does not open** (expected, not a fault) | `story.html` shows a dialog only if `/api/story` returns an `activeChoice`, and the backend currently always returns `null` there. The run does pause (pill `PAUSED`, counter stops at 57 and 135). | Answer from the second PowerShell window: `Answer CHOICE_FAMINE 0` (counter 57) and `Answer CHOICE_OUTBREAK 0` (counter 135), then click `RESUME`. Read the question and options from the script. Never click `STEP TICK` while a choice is pending: the step goes ahead anyway and the choice stays unanswered. |
| **Counter or chapter label looks stuck after `Step-To`** | The page only redraws the counter on a click or reload | Click `STEP TICK` once or press `F5`. State lives in the backend. |
| **Chose too early or too late** | Wrong `Step-To` target or an extra click | The run cannot go backwards. Keep talking from the current tick using the run map (section 1.2); restart the app only between rehearsals. |
| **Nothing moves after `START`** | By design, `START` does not tick (section 1.1) | Click `STEP TICK`. |
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

### Q6: "Why do I have to press STEP TICK? Why doesn't the simulation just run?"
**Answer:**
"Ticks are explicit commands to the `TickCoordinator`. That makes every run reproducible for a given seed and decision log, and it lets me stop on any tick for a decision or an inspection. The lifecycle (`RUNNING`, `PAUSED`) is real engine state, but in this build the tick clock is driven by step commands, not a free-running timer. For the demonstration I fast-forward with the same step endpoint instead of clicking 180 times."

### Q7: "What decides when a chapter changes, and what actually happened in your run?"
**Answer:**
"Chapter transitions are threshold-driven, evaluated at the tick boundary. Chapter II opens when the Observer's interventions produce visible discord (tick 46). Chapter III opens when Observer exposure reaches 60 (tick 66). Chapter IV opens when the population drops below 20 (tick 178). The hard backstop did not fire in the default run: each change had its real trigger (see the `chapterArc` in `/api/recap`). Because the world is deterministic, you get these same ticks every time with seed 42."

### Q8: "Food shows zero for most of the run and yet nobody dies for a long time. Why?"
**Answer:**
"Survivors forage steadily, about one ration per three ticks, so the stockpile is consumed as fast as it arrives. Famine and Disease each reduce vitality by one per tick; survivors die when vitality reaches zero, and the RESILIENT trait slows that, so the weakest go first and the strongest last. Deaths begin around tick 167 (5 dead by counter 178), after the outbreak at tick 135, and two survivors remain at tick 400."

### Q9: "Where is the Observer's manipulation, and can the player stop it?"
**Answer:**
"The Observer proposes `Intervention` commands from an immutable snapshot, applied at a defined tick boundary, never by mutating a survivor directly. Each one is archived on the Records page with the ground truth next to the altered report. In the default run the Observer uses two of its four policies, `ResourcePressure` (from tick 46) and `SelectiveDisclosure` (from tick 133). The Overseer's levers are the two choice moments and the evidence investigation."

---

## 6. Known Rough Edges (what could embarrass the demo)

Verified by running the packaged app. Docs-only change: none of these is fixed here.

1. **The choice dialog never opens** (section 4). The run pauses correctly, but `SimulationService.getStory()` returns `activeChoice = null`, so `story.js` has nothing to show. Workaround: `Answer ...` from PowerShell.
2. **`START` and `RESUME` do not advance time** (section 1.1). Only `STEP TICK` does.
3. **The Final Recap scene list is repetitive.** At tick 220 it runs to 71 lines, 66 of them "Day N. Observer record: ..." entries (checked by running the exact script) It has no deaths or character names. Show the tiles and the closing line only.
4. **The chronicle floods.** About 12 captions per tick, over 2,000 entries by tick 180, uncapped without PR 5. Do not scroll it; keep the focus on the counter, the label and the newest lines.
5. **Raw labels:** the chapter label reads `CHAPTER_2_THE_FRACTURE`, not "Chapter II". Say the proper name aloud.
6. **Live updates after an API step** are partial: captions stream in, but the counter and label wait for a click or `F5`.
7. **Interventions are already `APPLIED`.** There is no "approve directive" button; the Overseer's influence is the two choices and evidence discovery. Evidence discovery does not trigger a chapter.
8. **Food reads 0 or 1 from about tick 60 to 220, medicine 0 from tick 135.** Expected, but an evaluator may ask (Q8).

