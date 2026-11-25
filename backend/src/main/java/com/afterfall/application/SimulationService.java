package com.afterfall.application;

import com.afterfall.behavior.BehaviorSelector;
import com.afterfall.chapter.ChapterTransition;
import com.afterfall.choice.ChoiceRecord;
import com.afterfall.events.EventSubscriber;
import com.afterfall.events.SimEvent;
import com.afterfall.recap.FinalRecordingGenerator;
import java.time.Instant;
import com.afterfall.chapter.ChapterManager;
import com.afterfall.choice.ChoiceCoordinator;
import com.afterfall.choice.ChoiceMoment;
import com.afterfall.domain.*;
import com.afterfall.events.EventBus;
import com.afterfall.narrator.Narrator;
import com.afterfall.narrator.StoryCaption;
import com.afterfall.observer.InterventionRecord;
import com.afterfall.observer.ObserverAI;
import com.afterfall.application.persistence.EvidenceDiscoveryRepository;
import com.afterfall.application.persistence.EventRecorder;
import com.afterfall.application.persistence.RunCheckpoint;
import com.afterfall.application.persistence.RunId;
import com.afterfall.application.persistence.RunMetadata;
import com.afterfall.application.persistence.RunRecapInputs;
import com.afterfall.application.persistence.RunRepository;
import com.afterfall.recap.EvidenceItem;
import com.afterfall.recap.EvidenceService;
import com.afterfall.recap.RecapGenerator;
import com.afterfall.recap.RunRecap;
import com.afterfall.simulation.*;
import com.afterfall.web.dto.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Authoritative application service encapsulating the single active SimulationEngine,
 * Narrator, and associated domain components.
 */
@Service
public class SimulationService {

    private final SimulationEngine engine;
    private final World world;
    private final WorldClock clock;
    private final TickCoordinator coordinator;
    private final EventBus eventBus;
    private final Narrator narrator;
    private final ChoiceCoordinator choiceCoordinator;
    private final ChapterManager chapterManager;
    private final ObserverAI observerAI;
    private final RunRepository runRepository;
    private final EventRecorder eventRecorder;
    private final EvidenceDiscoveryRepository evidenceRepo;
    private final EvidenceService evidenceService;
    private final RecapGenerator recapGenerator;
    private final RunId currentRunId = RunId.of("run-1");

    private final List<Consumer<StoryCaption>> captionListeners = new CopyOnWriteArrayList<>();
    private int lastCaptionCount = 0;
    private int lastTransitionCount = 0;

    public SimulationService(RunRepository runRepository, EventRecorder eventRecorder, EvidenceDiscoveryRepository evidenceRepo) {
        this.runRepository = Objects.requireNonNull(runRepository, "runRepository");
        this.eventRecorder = Objects.requireNonNull(eventRecorder, "eventRecorder");
        this.evidenceRepo = Objects.requireNonNull(evidenceRepo, "evidenceRepo");
        this.evidenceService = new EvidenceService();
        this.recapGenerator = new RecapGenerator(evidenceService, new FinalRecordingGenerator());

        this.world = InitialPopulation.createDefaultWorld("Haven-1");
        this.clock = new WorldClock(4);
        this.eventBus = new EventBus();
        this.narrator = new Narrator(world.settlement(), new SeededRandomFactory(TickCoordinator.DEFAULT_RUN_SEED));
        this.choiceCoordinator = new ChoiceCoordinator();
        this.chapterManager = new ChapterManager();
        this.observerAI = new ObserverAI();

        // Save initial run metadata
        this.runRepository.saveRun(new RunMetadata(
                currentRunId,
                TickCoordinator.DEFAULT_RUN_SEED,
                world.settlement().population(),
                Instant.now(),
                null,
                chapterManager.currentChapter().name(),
                SimulationLifecycle.PAUSED.name()
        ));

        // Register narrator to EventBus
        this.eventBus.register(narrator);

        // Persist canonical events under THIS run's id. The recorder is always wrapped in a
        // run-bound subscriber: AsyncEventRecorder's own subscriber role uses a configured default
        // run id ("current-run"), which would store events apart from the run-1 captions.
        this.eventBus.register(new EventSubscriber() {
            @Override
            public String subscriberId() {
                return "event-recorder-" + currentRunId.value();
            }

            @Override
            public void onEvents(List<SimEvent> events) {
                eventRecorder.recordAll(currentRunId, events);
            }
        });

        this.coordinator = new TickCoordinator(
                world,
                clock,
                new BehaviorSelector()::selectAction,
                new RegionWorkerPool(),
                true,
                new SeededRandomFactory(TickCoordinator.DEFAULT_RUN_SEED),
                new CrisisManager(),
                observerAI,
                eventBus,
                choiceCoordinator,
                chapterManager
        );

        this.engine = new SimulationEngine(world, clock, coordinator);
    }

    public void addCaptionListener(Consumer<StoryCaption> listener) {
        this.captionListeners.add(listener);
    }

    public void addEventSubscriber(EventSubscriber subscriber) {
        this.eventBus.register(subscriber);
    }

    /**
     * Returns the internal EventBus for testing purposes only.
     * Production callers should use {@link #addEventSubscriber(EventSubscriber)}.
     */
    EventBus eventBus() {
        return eventBus;
    }

    public synchronized StateResponse getCurrentState() {
        long tick = clock.tick();
        String chapter = chapterManager.currentChapter().name();
        SimulationLifecycle status = engine.getStatus();
        boolean hasPending = choiceCoordinator.hasPendingPause();

        // Build resource map
        Settlement s = world.settlement();
        Map<String, Integer> resources = new LinkedHashMap<>();
        for (ResourceType rt : ResourceType.values()) {
            resources.put(rt.name().toLowerCase(), s.resourceStock().resource(rt).quantity());
        }

        int alive = (int) s.survivors().stream().filter(Survivor::isAlive).count();
        int dead = s.survivors().size() - alive;

        return new StateResponse("run-1", tick, status.name(), chapter, alive, dead, resources, hasPending, hasPending);
    }

    public synchronized StateResponse startSimulation() {
        engine.start();
        return getCurrentState();
    }

    public synchronized StateResponse pauseSimulation() {
        engine.pause();
        return getCurrentState();
    }

    public synchronized StateResponse resumeSimulation() {
        engine.resume();
        return getCurrentState();
    }

    public synchronized StateResponse stepTick() {
        if (engine.getStatus() == SimulationLifecycle.PAUSED) {
            engine.resume();
            engine.tick();
            engine.pause();
        } else {
            engine.tick();
        }

        // Notify caption listeners of newly generated captions and persist them
        List<StoryCaption> allCaptions = narrator.captions();
        if (allCaptions.size() > lastCaptionCount) {
            for (int i = lastCaptionCount; i < allCaptions.size(); i++) {
                StoryCaption c = allCaptions.get(i);
                runRepository.recordCaption(currentRunId, c);
                for (Consumer<StoryCaption> l : captionListeners) {
                    try {
                        l.accept(c);
                    } catch (Exception ignored) {}
                }
            }
            lastCaptionCount = allCaptions.size();
        }

        // Persist chapter transitions, choices, and checkpoints
        List<ChapterTransition> transitions = chapterManager.transitionHistory();
        for (int i = lastTransitionCount; i < transitions.size(); i++) {
            runRepository.recordChapterTransition(currentRunId, transitions.get(i));
        }
        lastTransitionCount = transitions.size();
        for (ChoiceRecord cr : choiceCoordinator.decisionLog()) {
            runRepository.recordChoice(currentRunId, cr);
        }
        for (InterventionRecord ir : observerAI.records()) {
            runRepository.recordIntervention(currentRunId, ir);
        }

        // Extract and persist available evidence
        List<EvidenceItem> available = evidenceService.extractAvailableEvidence(observerAI.records());
        evidenceRepo.saveAllAvailableEvidence(currentRunId, available);

        // Checkpoint macro metrics
        Settlement set = world.settlement();
        int alive = (int) set.survivors().stream().filter(Survivor::isAlive).count();
        int dead = set.population() - alive;
        runRepository.saveCheckpoint(new RunCheckpoint(
                currentRunId,
                clock.tick(),
                alive,
                dead,
                set.resourceStock().resource(ResourceType.FOOD).quantity(),
                set.resourceStock().resource(ResourceType.WATER).quantity(),
                set.resourceStock().resource(ResourceType.MEDICINE).quantity(),
                chapterManager.currentChapter().name(),
                Instant.now()
        ));

        return getCurrentState();
    }

    public List<SurvivorDto> getAllSurvivors() {
        List<SurvivorDto> result = new ArrayList<>();
        Settlement settlement = world.settlement();
        for (Survivor s : settlement.survivors()) {
            result.add(toSurvivorDto(s, settlement));
        }
        return result;
    }

    public Optional<SurvivorDto> getSurvivor(String id) {
        try {
            SurvivorId sid = new SurvivorId(Integer.parseInt(id));
            Settlement settlement = world.settlement();
            return settlement.survivor(sid).map(s -> toSurvivorDto(s, settlement));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public StoryResponse getStory() {
        List<StoryResponse.CaptionDto> captionDtos = narrator.captions().stream()
                .map(c -> new StoryResponse.CaptionDto(
                        c.captionId(),
                        c.tick(),
                        c.text(),
                        c.severity().name(),
                        c.sourceEventIds().stream().map(EventId::value).toList()
                ))
                .toList();
        return new StoryResponse(chapterManager.currentChapter().name(), clock.tick(), captionDtos, null);
    }

    public synchronized StateResponse choose(String choiceId, int optionIndex) {
        Optional<ChoiceMoment> pending = choiceCoordinator.pendingChoice();
        if (pending.isPresent() && pending.get().id().equals(choiceId)) {
            ChoiceMoment moment = pending.get();
            String optionId = null;
            if (optionIndex >= 0 && optionIndex < moment.options().size()) {
                optionId = moment.options().get(optionIndex).optionId();
            }
            choiceCoordinator.submitDecision(choiceId, optionId);
        }
        return getCurrentState();
    }

    public RecordsResponse getRecords() {
        List<InterventionRecord> records = observerAI.records();
        List<RecordsResponse.InterventionAuditDto> dtos = records.stream()
                .map(r -> new RecordsResponse.InterventionAuditDto(
                        r.interventionId(),
                        r.tick(),
                        r.type().name(),
                        r.targetDescription(),
                        r.reason(),
                        r.originalReport(),
                        r.alteredReport(),
                        r.status().name(),
                        r.actualResult()
                ))
                .toList();
        return new RecordsResponse(dtos);
    }

    public List<EvidenceItem> getAvailableEvidence() {
        // Sync any newly produced evidence from recent interventions
        List<EvidenceItem> extracted = evidenceService.extractAvailableEvidence(observerAI.records());
        evidenceRepo.saveAllAvailableEvidence(currentRunId, extracted);
        return evidenceRepo.availableEvidenceForRun(currentRunId);
    }

    public List<EvidenceItem> getDiscoveredEvidence() {
        return evidenceRepo.discoveredEvidenceForRun(currentRunId);
    }

    public Set<String> getDiscoveredEvidenceIds() {
        return evidenceRepo.discoveredEvidenceIdsForRun(currentRunId);
    }

    public synchronized boolean discoverEvidence(String evidenceId) {
        boolean newlyDiscovered = evidenceRepo.markDiscovered(currentRunId, evidenceId);
        if (newlyDiscovered) {
            // Integrate with Chapter III discovery threshold
            chapterManager.recordEvidenceDiscovered();
        }
        return newlyDiscovered;
    }

    public RunRecap getRecap() {
        // 1. Gather all available evidence from current state
        List<EvidenceItem> extracted = evidenceService.extractAvailableEvidence(observerAI.records());
        evidenceRepo.saveAllAvailableEvidence(currentRunId, extracted);

        // 2. Load recap inputs from persistence
        RunRecapInputs inputs = runRepository.loadRecapInputs(currentRunId);

        // 3. Create immutable snapshots of current survivors to fulfill zero-live-dependency requirement
        List<SurvivorSnapshot> snapshots = world.settlement().survivors().stream()
                .map(SurvivorSnapshot::from)
                .toList();

        RunRecapInputs completeInputs = new RunRecapInputs(
                inputs.runId(),
                inputs.metadata(),
                inputs.chapterTransitions(),
                inputs.choices(),
                inputs.interventions(),
                inputs.captions(),
                inputs.checkpoints(),
                snapshots
        );

        return recapGenerator.generate(completeInputs, evidenceRepo.discoveredEvidenceIdsForRun(currentRunId));
    }

    public void approveIntervention(String id) {
        // Observer intervention approval (foundation for M10/M11)
    }

    public void rejectIntervention(String id) {
        // Observer intervention rejection (foundation for M10/M11)
    }

    public MetricsResponse getMetrics() {
        Settlement settlement = world.settlement();
        ResourceStock stock = settlement.resourceStock();

        int food = stock.resource(ResourceType.FOOD).quantity();
        int water = stock.resource(ResourceType.WATER).quantity();
        int medicine = stock.resource(ResourceType.MEDICINE).quantity();

        int alive = 0;
        int dead = 0;

        for (Survivor s : settlement.survivors()) {
            if (s.isAlive()) {
                alive++;
            } else {
                dead++;
            }
        }

        return new MetricsResponse(clock.tick(), engine.getStatus().name(), alive, dead, food, water, medicine, 0, 0L, false);
    }

    private SurvivorDto toSurvivorDto(Survivor s, Settlement settlement) {
        int regId = settlement.regionOf(s.id()).map(RegionId::value).orElse(0);

        // Find dominant trait
        Trait dominant = Trait.COOPERATIVE;
        int maxVal = -1;
        for (Trait t : Trait.values()) {
            int val = s.personality().value(t);
            if (val > maxVal) {
                maxVal = val;
                dominant = t;
            }
        }

        List<String> memorySummaries = s.memories().stream()
                .map(m -> "Day " + m.day() + ": Impact " + m.emotionalImpact() + " [Event #" + m.sourceEventId().value() + "]")
                .toList();

        SurvivorDto.MapDto needs = new SurvivorDto.MapDto(
                s.needs().level(Need.FOOD),
                s.needs().level(Need.WATER),
                s.needs().level(Need.REST),
                s.health().vitality()
        );

        return new SurvivorDto(
                (int) s.id().value(),
                regId,
                s.isAlive(),
                s.profession().type().name(),
                dominant.name(),
                needs,
                50,
                List.of(),
                memorySummaries
        );
    }
}
