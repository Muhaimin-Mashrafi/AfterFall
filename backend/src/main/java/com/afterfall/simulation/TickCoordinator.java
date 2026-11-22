package com.afterfall.simulation;

import com.afterfall.action.Action;
import com.afterfall.action.ActionContext;
import com.afterfall.action.ActionResult;
import com.afterfall.action.ProposedEvent;
import com.afterfall.action.ResourceClaim;
import com.afterfall.behavior.BehaviorContext;
import com.afterfall.behavior.BehaviorSelector;
import com.afterfall.domain.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * Coordinates one deterministic simulation tick across all four regions.
 *
 * <p>Six-phase protocol:
 * <ol>
 *   <li>Validate inputs; prevent overlapping ticks via atomic guard.</li>
 *   <li>Capture exactly one immutable {@link WorldView} for the tick.</li>
 *   <li>Evaluate regions in {@link RegionId} order; survivors in {@link SurvivorId} order.</li>
 *   <li>Validate every proposal before applying any mutation.</li>
 *   <li>Apply accepted proposals via {@link StateChangeApplier#applyBatch}.</li>
 *   <li>Advance {@link WorldClock}, increment explicit world version, and assemble {@link TickResult}.</li>
 * </ol>
 *
 * <p>No threads, ExecutorService, or CompletableFuture are used.
 * ResourceClaims and migration requests are preserved in the result but NOT resolved.
 */
public final class TickCoordinator {

    private final World world;
    private final WorldClock clock;
    private final Function<BehaviorContext, Action> actionDecider;
    private final AtomicBoolean tickGuard = new AtomicBoolean(false);
    private long worldVersion = 0L;

    public TickCoordinator(World world, WorldClock clock) {
        this(world, clock, new BehaviorSelector());
    }

    public TickCoordinator(World world, WorldClock clock, BehaviorSelector behaviorSelector) {
        this.world = Objects.requireNonNull(world, "world");
        this.clock = Objects.requireNonNull(clock, "clock");
        Objects.requireNonNull(behaviorSelector, "behaviorSelector");
        this.actionDecider = behaviorSelector::selectAction;
    }

    public TickCoordinator(World world, WorldClock clock, Function<BehaviorContext, Action> actionDecider) {
        this.world = Objects.requireNonNull(world, "world");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.actionDecider = Objects.requireNonNull(actionDecider, "actionDecider");
    }

    public long worldVersion() {
        return worldVersion;
    }

    /**
     * Execute one complete simulation tick.
     *
     * @return an immutable {@link TickResult} describing what occurred.
     * @throws IllegalStateException if a tick is already in progress.
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

        // Phase 2: build one immutable WorldView for the entire tick
        WorldView worldView = WorldView.snapshot(world, currentTick, currentVersion);

        // Membership Consistency Validation: Every resident survivor in each region must exist in Settlement and WorldView
        for (RegionId regionId : RegionId.ALL) {
            RegionView regionView = worldView.regions().get(regionId);
            if (regionView != null) {
                for (SurvivorId residentId : regionView.survivorIds()) {
                    if (settlement.survivor(residentId).isEmpty() || !worldView.survivors().containsKey(residentId)) {
                        return TickResult.builder(currentTick, currentVersion)
                                .failed("Membership inconsistency: survivor " + residentId + " in region " + regionId + " roster not found in Settlement/WorldView")
                                .build();
                    }
                }
            }
        }

        // Phase 3: evaluate regions in RegionId order, survivors in SurvivorId order
        List<RegionResult> regionResults = new ArrayList<>();
        List<ProposedEvent> allEvents = new ArrayList<>();
        List<ResourceClaim> allClaims = new ArrayList<>();
        List<MigrationRequest> allMigrationRequests = new ArrayList<>();

        LinkedHashMap<Survivor, ActionResult> proposalsToApply = new LinkedHashMap<>();

        int sequence = 0;
        int accepted = 0;
        int rejected = 0;
        int survivorsProcessed = 0;

        for (RegionId regionId : RegionId.ALL) {
            RegionView regionView = worldView.regions().get(regionId);
            if (regionView == null) {
                continue;
            }

            List<SurvivorActionResult> survivorResults = new ArrayList<>();
            List<MigrationRequest> regionMigrationRequests = new ArrayList<>();

            // Sort survivor IDs deterministically
            List<SurvivorId> orderedSurvivorIds = new ArrayList<>(new TreeSet<>(regionView.survivorIds()));

            for (SurvivorId survivorId : orderedSurvivorIds) {
                Optional<Survivor> liveSurvivorOpt = settlement.survivor(survivorId);
                if (liveSurvivorOpt.isEmpty()) {
                    return TickResult.builder(currentTick, currentVersion)
                            .failed("Membership inconsistency during evaluation: survivor " + survivorId + " missing from settlement")
                            .build();
                }
                Survivor liveSurvivor = liveSurvivorOpt.get();

                SurvivorView survivorView = worldView.survivors().get(survivorId);
                if (survivorView == null) {
                    return TickResult.builder(currentTick, currentVersion)
                            .failed("Membership inconsistency during evaluation: survivor " + survivorId + " missing from WorldView")
                            .build();
                }

                // Build SurvivorSnapshot from the immutable view
                SurvivorSnapshot snapshot = new SurvivorSnapshot(
                        survivorView.id(),
                        survivorView.name(),
                        survivorView.personality(),
                        survivorView.profession(),
                        survivorView.needLevels(),
                        survivorView.vitality(),
                        survivorView.condition(),
                        survivorView.inventoryQuantities(),
                        survivorView.inventoryCapacity(),
                        survivorView.location(),
                        survivorView.status()
                );

                survivorsProcessed++;

                BehaviorContext behaviorContext = BehaviorContext.of(
                        snapshot,
                        regionId,
                        false,  // shared food: M5, never grant free food
                        false   // shared medicine: M5, never grant free medicine
                );

                Action action = actionDecider.apply(behaviorContext);

                ActionContext actionContext = new ActionContext(
                        snapshot,
                        regionId,
                        false,
                        false
                );

                ActionResult result = action.evaluate(actionContext);

                sequence++;
                SurvivorActionResult sar = new SurvivorActionResult(survivorId, sequence, result);
                survivorResults.add(sar);

                allEvents.addAll(result.eventProposals());
                allClaims.addAll(result.resourceClaims());

                final int actionSeq = sequence;
                result.migrationRequest().ifPresent(dest -> {
                    MigrationRequest req = new MigrationRequest(survivorId, regionId, dest, actionSeq);
                    regionMigrationRequests.add(req);
                    allMigrationRequests.add(req);
                });

                if (result.isSuccess()) {
                    accepted++;
                    proposalsToApply.put(liveSurvivor, result);
                } else {
                    rejected++;
                }
            }

            regionResults.add(new RegionResult(regionId, List.copyOf(survivorResults), List.copyOf(regionMigrationRequests)));
        }

        // Phase 4 & 5: validate and apply proposals atomically via applyBatch
        try {
            if (!proposalsToApply.isEmpty()) {
                StateChangeApplier.applyBatch(proposalsToApply);
            }
        } catch (RuntimeException e) {
            // Application failed – clock and worldVersion must NOT advance; return failure result
            return TickResult.builder(currentTick, currentVersion)
                    .regionResults(regionResults)
                    .events(allEvents)
                    .resourceClaims(allClaims)
                    .migrationRequests(allMigrationRequests)
                    .survivorsProcessed(survivorsProcessed)
                    .actionsAccepted(accepted)
                    .actionsRejected(rejected)
                    .failed("Batch application failed: " + e.getMessage())
                    .build();
        }

        // Phase 6: advance clock and increment worldVersion ONLY on success
        clock.advance();
        this.worldVersion++;
        long newTick = clock.tick();
        long newVersion = this.worldVersion;

        return TickResult.builder(newTick, newVersion)
                .regionResults(regionResults)
                .events(allEvents)
                .resourceClaims(allClaims)
                .migrationRequests(allMigrationRequests)
                .survivorsProcessed(survivorsProcessed)
                .actionsAccepted(accepted)
                .actionsRejected(rejected)
                .build();
    }
}
