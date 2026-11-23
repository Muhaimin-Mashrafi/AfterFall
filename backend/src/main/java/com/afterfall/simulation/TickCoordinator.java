package com.afterfall.simulation;

import com.afterfall.action.Action;
import com.afterfall.action.ActionResult;
import com.afterfall.action.ProposedEvent;
import com.afterfall.action.ResourceClaim;
import com.afterfall.behavior.BehaviorContext;
import com.afterfall.behavior.BehaviorSelector;
import com.afterfall.domain.*;
import com.afterfall.observer.Intervention;
import com.afterfall.observer.InterventionRecord;
import com.afterfall.observer.InterventionStatus;
import com.afterfall.observer.ObserverAI;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * Coordinates one deterministic simulation tick using the M4 four-worker parallel architecture.
 *
 * <p>Six-phase protocol:
 * <ol>
 *   <li><b>Phase 1: Validate</b> — Check reentrancy guard, capture tick and version, validate membership consistency.</li>
 *   <li><b>Phase 2: Snapshot</b> — Create exactly one immutable {@link WorldView} instance.</li>
 *   <li><b>Phase 3: Parallel Evaluation</b> — Submit exactly 4 {@link RegionTask}s to {@link RegionWorkerPool}.</li>
 *   <li><b>Phase 4: Validate Results</b> — Check that all 4 results completed without error, matching expected regions.</li>
 *   <li><b>Phase 5: Deterministic Merge</b> — Sort results by {@link RegionId}, survivors by {@link SurvivorId}.
 *       Assign global event sequence numbers, resolve cross-region migrations via {@link MigrationCoordinator},
 *       and atomically apply state proposals via {@link StateChangeApplier#applyBatch}.</li>
 *   <li><b>Phase 6: Complete</b> — Advance {@link WorldClock}, increment version, build {@link TickResult},
 *       and release tick guard.</li>
 * </ol>
 */
public final class TickCoordinator {

    public static final long DEFAULT_RUN_SEED = 42L;

    private final World world;
    private final WorldClock clock;
    private final Function<BehaviorContext, Action> actionDecider;
    private final RegionWorkerPool workerPool;
    private final boolean ownsWorkerPool;
    private final SeededRandomFactory randomFactory;
    private final MigrationCoordinator migrationCoordinator;
    private final CrisisManager crisisManager;
    private final ObserverAI observerAI;
    private final Queue<Intervention> interventionQueue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean tickGuard = new AtomicBoolean(false);
    private long worldVersion = 0L;
    private long nextGlobalEventSequence = 1L;

    public TickCoordinator(World world, WorldClock clock) {
        this(world, clock, new BehaviorSelector());
    }

    public TickCoordinator(World world, WorldClock clock, long runSeed) {
        this(world, clock, new BehaviorSelector(), runSeed);
    }

    public TickCoordinator(World world, WorldClock clock, BehaviorSelector behaviorSelector) {
        this(world, clock, Objects.requireNonNull(behaviorSelector, "behaviorSelector")::selectAction);
    }

    public TickCoordinator(World world, WorldClock clock, BehaviorSelector behaviorSelector, long runSeed) {
        this(world, clock, Objects.requireNonNull(behaviorSelector, "behaviorSelector")::selectAction,
                new RegionWorkerPool(), true, new SeededRandomFactory(runSeed));
    }

    public TickCoordinator(World world, WorldClock clock, Function<BehaviorContext, Action> actionDecider) {
        this(world, clock, actionDecider, new RegionWorkerPool(), true, new SeededRandomFactory(DEFAULT_RUN_SEED));
    }

    public TickCoordinator(World world, WorldClock clock, Function<BehaviorContext, Action> actionDecider, long runSeed) {
        this(world, clock, actionDecider, new RegionWorkerPool(), true, new SeededRandomFactory(runSeed));
    }

    public TickCoordinator(World world,
                           WorldClock clock,
                           Function<BehaviorContext, Action> actionDecider,
                           RegionWorkerPool workerPool,
                           boolean ownsWorkerPool,
                           SeededRandomFactory randomFactory) {
        this(world, clock, actionDecider, workerPool, ownsWorkerPool, randomFactory, new CrisisManager());
    }

    public TickCoordinator(World world,
                           WorldClock clock,
                           Function<BehaviorContext, Action> actionDecider,
                           RegionWorkerPool workerPool,
                           boolean ownsWorkerPool,
                           SeededRandomFactory randomFactory,
                           CrisisManager crisisManager) {
        this(world, clock, actionDecider, workerPool, ownsWorkerPool, randomFactory, crisisManager, new ObserverAI());
    }

    public TickCoordinator(World world,
                           WorldClock clock,
                           Function<BehaviorContext, Action> actionDecider,
                           RegionWorkerPool workerPool,
                           boolean ownsWorkerPool,
                           SeededRandomFactory randomFactory,
                           CrisisManager crisisManager,
                           ObserverAI observerAI) {
        this.world = Objects.requireNonNull(world, "world");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.actionDecider = Objects.requireNonNull(actionDecider, "actionDecider");
        this.workerPool = Objects.requireNonNull(workerPool, "workerPool");
        this.ownsWorkerPool = ownsWorkerPool;
        this.randomFactory = Objects.requireNonNull(randomFactory, "randomFactory");
        this.migrationCoordinator = new MigrationCoordinator(world.settlement());
        this.crisisManager = Objects.requireNonNull(crisisManager, "crisisManager");
        this.observerAI = Objects.requireNonNull(observerAI, "observerAI");
    }

    public long worldVersion() {
        return worldVersion;
    }

    public RegionWorkerPool workerPool() {
        return workerPool;
    }

    public SeededRandomFactory randomFactory() {
        return randomFactory;
    }

    /**
     * Executes one complete deterministic simulation tick using 4 parallel region workers.
     *
     * @return an immutable {@link TickResult}
     * @throws IllegalStateException if a tick is already in progress
     */
    public TickResult executeTick() {
        // Phase 1: validate / guard against overlapping ticks atomically
        if (!tickGuard.compareAndSet(false, true)) {
            throw new IllegalStateException("A tick is already in progress");
        }
        try {
            return doExecute();
        } finally {
            tickGuard.set(false);
        }
    }

    private TickResult doExecute() {
        Settlement settlement = world.settlement();
        long currentTick = clock.tick();
        long currentVersion = this.worldVersion;

        // Phase 1 validation: verify all resident survivors in each region exist in settlement
        for (RegionId regionId : RegionId.ALL) {
            Optional<Region> rOpt = settlement.region(regionId);
            if (rOpt.isEmpty()) {
                return TickResult.builder(currentTick, currentVersion)
                        .failed("Missing required region: " + regionId)
                        .build();
            }
            Region region = rOpt.get();
            for (SurvivorId residentId : region.survivorIds()) {
                if (settlement.survivor(residentId).isEmpty()) {
                    return TickResult.builder(currentTick, currentVersion)
                            .failed("Membership inconsistency: survivor " + residentId + " in region " + regionId + " roster not found in Settlement/WorldView")
                            .build();
                }
            }
        }

        // Phase 2: build one immutable WorldView for the entire tick
        WorldView worldView = WorldView.snapshot(world, currentTick, currentVersion);

        // Phase 3: Parallel regional evaluation
        List<RegionTask> tasks = new ArrayList<>(RegionWorkerPool.WORKER_COUNT);
        for (RegionId regionId : RegionId.ALL) {
            SplittableRandom rng = randomFactory.createForRegion(regionId, currentTick);
            tasks.add(new RegionTask(regionId, worldView, currentTick, rng, actionDecider));
        }

        List<Future<RegionResult>> futures;
        try {
            futures = workerPool.invokeAll(tasks, 5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return TickResult.builder(currentTick, currentVersion)
                    .failed("Tick execution interrupted: " + e.getMessage())
                    .build();
        }

        // Phase 4: Validate worker results
        List<RegionResult> rawResults = new ArrayList<>();
        for (Future<RegionResult> f : futures) {
            try {
                if (f.isCancelled()) {
                    return TickResult.builder(currentTick, currentVersion)
                            .failed("Region worker task was cancelled")
                            .build();
                }
                rawResults.add(f.get());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return TickResult.builder(currentTick, currentVersion)
                        .failed("Interrupted while collecting region worker results: " + e.getMessage())
                        .build();
            } catch (ExecutionException e) {
                return TickResult.builder(currentTick, currentVersion)
                        .failed("Region worker evaluation failed: " + e.getCause().getMessage())
                        .build();
            }
        }

        if (rawResults.size() != RegionWorkerPool.WORKER_COUNT) {
            return TickResult.builder(currentTick, currentVersion)
                    .failed("Expected " + RegionWorkerPool.WORKER_COUNT + " region results, got " + rawResults.size())
                    .build();
        }

        // Phase 5: Deterministic Merge
        // 5a. Explicit canonical ordering by RegionId
        rawResults.sort(Comparator.comparing(RegionResult::regionId));

        List<RegionResult> orderedRegionResults = new ArrayList<>();
        List<ProposedEvent> allEvents = new ArrayList<>();
        List<ResourceClaim> allClaims = new ArrayList<>();
        List<MigrationRequest> allMigrationRequests = new ArrayList<>();
        LinkedHashMap<Survivor, ActionResult> proposalsToApply = new LinkedHashMap<>();

        int survivorsProcessed = 0;
        int accepted = 0;
        int rejected = 0;
        Set<SurvivorId> evaluatedSurvivors = new HashSet<>();

        for (RegionResult rr : rawResults) {
            RegionId regionId = rr.regionId();
            List<SurvivorActionResult> sortedSurvivorResults = new ArrayList<>(rr.survivorResults());
            // Sort survivors by SurvivorId
            sortedSurvivorResults.sort(Comparator.comparing(SurvivorActionResult::survivorId));

            for (SurvivorActionResult sar : sortedSurvivorResults) {
                SurvivorId sId = sar.survivorId();
                if (!evaluatedSurvivors.add(sId)) {
                    return TickResult.builder(currentTick, currentVersion)
                            .failed("Duplicate survivor evaluation detected: " + sId)
                            .build();
                }

                Optional<Survivor> liveSurvivorOpt = settlement.survivor(sId);
                if (liveSurvivorOpt.isEmpty()) {
                    return TickResult.builder(currentTick, currentVersion)
                            .failed("Survivor " + sId + " missing from settlement")
                            .build();
                }
                Survivor liveSurvivor = liveSurvivorOpt.get();

                survivorsProcessed++;
                ActionResult result = sar.actionResult();

                // Assign global sequential event numbering during canonical merge
                for (ProposedEvent pe : result.eventProposals()) {
                    allEvents.add(pe);
                    nextGlobalEventSequence++;
                }

                // Preserve resource claims
                allClaims.addAll(result.resourceClaims());

                if (result.isSuccess()) {
                    accepted++;
                    proposalsToApply.put(liveSurvivor, result);
                } else {
                    rejected++;
                }
            }

            allMigrationRequests.addAll(rr.migrationRequests());
            orderedRegionResults.add(new RegionResult(regionId, sortedSurvivorResults, rr.migrationRequests()));
        }

        // 5b. Resolve and execute validated migration requests at tick boundary
        List<MigrationCoordinator.AppliedMigration> appliedMigrations;
        try {
            appliedMigrations = migrationCoordinator.applyMigrations(allMigrationRequests);
        } catch (RuntimeException e) {
            return TickResult.builder(currentTick, currentVersion)
                    .regionResults(orderedRegionResults)
                    .events(allEvents)
                    .resourceClaims(allClaims)
                    .migrationRequests(allMigrationRequests)
                    .survivorsProcessed(survivorsProcessed)
                    .actionsAccepted(accepted)
                    .actionsRejected(rejected)
                    .failed("Migration validation/handoff failed: " + e.getMessage())
                    .build();
        }

        // 5c. Atomically apply domain state proposals via StateChangeApplier.applyBatch
        try {
            if (!proposalsToApply.isEmpty()) {
                StateChangeApplier.applyBatch(proposalsToApply);
            }
        } catch (RuntimeException e) {
            // Roll back migrations on domain state application failure
            migrationCoordinator.rollbackMigrations(appliedMigrations);
            return TickResult.builder(currentTick, currentVersion)
                    .regionResults(orderedRegionResults)
                    .events(allEvents)
                    .resourceClaims(allClaims)
                    .migrationRequests(allMigrationRequests)
                    .survivorsProcessed(survivorsProcessed)
                    .actionsAccepted(accepted)
                    .actionsRejected(rejected)
                    .failed("Batch state application failed: " + e.getMessage())
                    .build();
        }

        // 5d. Resolve resource claims against settlement stock in deterministic (region, survivor) order
        ResourceStock stock = settlement.resourceStock();
        for (ResourceClaim claim : allClaims) {
            stock.resource(mapToResourceType(claim.resourceType())).consume(claim.quantity());
        }

        // 5e. Advance crises and apply their effects as the sole writer
        List<CrisisEffect> crisisEffects = crisisManager.advanceAll(settlement, stock);
        for (CrisisEffect effect : crisisEffects) {
            settlement.survivor(effect.targetId()).ifPresent(survivor -> {
                for (Map.Entry<Need, Integer> e : effect.needDeltas().entrySet()) {
                    survivor.needs().adjust(e.getKey(), e.getValue());
                }
                if (effect.vitalityDelta() != 0) {
                    survivor.health().adjustVitality(effect.vitalityDelta());
                }
                effect.newCondition().ifPresent(survivor.health()::setCondition);
            });
        }

        // 5f. Apply queued Observer interventions at safe tick boundary
        applyQueuedInterventions(settlement, currentTick);

        // Phase 6: Advance time and increment version
        clock.advance();
        this.worldVersion++;
        long newTick = clock.tick();
        long newVersion = this.worldVersion;

        return TickResult.builder(newTick, newVersion)
                .regionResults(orderedRegionResults)
                .events(allEvents)
                .resourceClaims(allClaims)
                .migrationRequests(allMigrationRequests)
                .survivorsProcessed(survivorsProcessed)
                .actionsAccepted(accepted)
                .actionsRejected(rejected)
                .build();
    }

    /**
     * Cleans up resources owned by this coordinator.
     */
    public void close() {
        if (ownsWorkerPool) {
            workerPool.close();
        }
    }

    /**
     * Alias for close() providing standard shutdown semantics.
     */
    public void shutdown() {
        close();
    }

    public CrisisManager crisisManager() {
        return crisisManager;
    }

    public ObserverAI observerAI() {
        return observerAI;
    }

    /**
     * Submits an intervention command to the queue to be applied at the next safe tick boundary.
     */
    public void queueIntervention(Intervention intervention) {
        interventionQueue.offer(Objects.requireNonNull(intervention, "intervention"));
    }

    private void applyQueuedInterventions(Settlement settlement, long currentTick) {
        List<Intervention> toProcess = new ArrayList<>();
        Intervention item;
        while ((item = interventionQueue.poll()) != null) {
            toProcess.add(item);
        }

        // Deterministic ordering by intervention ID
        toProcess.sort(Comparator.comparingLong(Intervention::id));

        for (Intervention intervention : toProcess) {
            applySingleIntervention(settlement, intervention, currentTick);
        }
    }

    private void applySingleIntervention(Settlement settlement, Intervention intervention, long currentTick) {
        // Validation: if target survivor specified, must exist and be alive
        if (intervention.targetSurvivor().isPresent()) {
            SurvivorId tid = intervention.targetSurvivor().get();
            Optional<Survivor> sOpt = settlement.survivor(tid);
            if (sOpt.isEmpty() || !sOpt.get().isAlive()) {
                observerAI.record(intervention, InterventionStatus.REJECTED,
                        "Target survivor " + tid.value() + " does not exist or is deceased.");
                return;
            }
        }

        // Apply state changes as sole writer
        if (intervention.targetSurvivor().isPresent()) {
            Survivor target = settlement.survivor(intervention.targetSurvivor().get()).get();
            for (Map.Entry<Need, Integer> entry : intervention.needDeltas().entrySet()) {
                target.needs().adjust(entry.getKey(), entry.getValue());
            }

            // Relationship adjustment if specified
            if (intervention.trustDelta() != 0 && intervention.relatedSurvivor().isPresent()) {
                SurvivorId relId = intervention.relatedSurvivor().get();
                target.relationshipWith(relId).ifPresent(rel -> {
                    rel.adjust((int) currentTick, intervention.trustDelta(), 0, -intervention.trustDelta(), new EventId(nextGlobalEventSequence++));
                });
            }
        } else if (intervention.targetRegion().isPresent()) {
            // Apply to all alive survivors in the target region
            RegionId rid = intervention.targetRegion().get();
            Optional<Region> regionOpt = settlement.region(rid);
            if (regionOpt.isPresent()) {
                for (SurvivorId sid : regionOpt.get().survivorIds()) {
                    settlement.survivor(sid).ifPresent(s -> {
                        if (s.isAlive()) {
                            for (Map.Entry<Need, Integer> entry : intervention.needDeltas().entrySet()) {
                                s.needs().adjust(entry.getKey(), entry.getValue());
                            }
                        }
                    });
                }
            }
        }

        observerAI.record(intervention, InterventionStatus.APPLIED,
                "Applied at tick " + currentTick + " boundary with intended effect: " + intervention.intendedEffect());
    }

    /**
     * Maps a ResourceClaim resource type to domain ResourceType.
     * Both share the same domain type; this call is a checked identity bridge for readability.
     */
    private static ResourceType mapToResourceType(ResourceType resourceType) {
        return resourceType;
    }
}
