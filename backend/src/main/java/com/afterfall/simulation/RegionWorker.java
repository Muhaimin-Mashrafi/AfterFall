package com.afterfall.simulation;

import com.afterfall.action.Action;
import com.afterfall.action.ActionContext;
import com.afterfall.action.ActionResult;
import com.afterfall.behavior.BehaviorContext;
import com.afterfall.domain.RegionId;
import com.afterfall.domain.SurvivorId;
import com.afterfall.domain.SurvivorSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.SplittableRandom;
import java.util.TreeSet;
import java.util.function.Function;

/**
 * Worker responsible for evaluating survivor behaviors and actions within a single region.
 *
 * <p>Strict worker contract:
 * <ul>
 *   <li>Reads only from the immutable {@link WorldView} and {@link RegionView}.</li>
 *   <li>Never mutates domain models or shared world state.</li>
 *   <li>Processes resident survivors in ascending {@link SurvivorId} order.</li>
 *   <li>Evaluates actions and packages proposals into an immutable {@link RegionResult}.</li>
 *   <li>Does not assign global event sequence numbers or resolve resource claims.</li>
 * </ul>
 */
public final class RegionWorker {

    private final RegionId regionId;
    private final WorldView worldView;
    private final long tick;
    private final SplittableRandom rng;
    private final Function<BehaviorContext, Action> actionDecider;

    public RegionWorker(RegionId regionId,
                        WorldView worldView,
                        long tick,
                        SplittableRandom rng,
                        Function<BehaviorContext, Action> actionDecider) {
        this.regionId = Objects.requireNonNull(regionId, "regionId");
        this.worldView = Objects.requireNonNull(worldView, "worldView");
        this.tick = tick;
        this.rng = Objects.requireNonNull(rng, "rng");
        this.actionDecider = Objects.requireNonNull(actionDecider, "actionDecider");
    }

    /**
     * Evaluates all resident survivors in this region deterministically.
     *
     * @return an immutable {@link RegionResult}
     */
    public RegionResult evaluate() {
        RegionView regionView = worldView.regions().get(regionId);
        if (regionView == null) {
            return new RegionResult(regionId, List.of(), List.of());
        }

        List<SurvivorActionResult> survivorResults = new ArrayList<>();
        List<MigrationRequest> migrationRequests = new ArrayList<>();

        // Deterministic survivor ordering
        List<SurvivorId> orderedSurvivorIds = new ArrayList<>(new TreeSet<>(regionView.survivorIds()));

        int localActionSequence = 0;

        for (SurvivorId survivorId : orderedSurvivorIds) {
            SurvivorView survivorView = worldView.survivors().get(survivorId);
            if (survivorView == null) {
                throw new IllegalStateException("Survivor " + survivorId + " in region " + regionId + " not found in WorldView");
            }

            // Build snapshot from immutable WorldView
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

            // Candidate regions for migration decision (the other 3 regions)
            List<RegionId> candidates = RegionId.ALL.stream()
                    .filter(id -> !id.equals(regionId))
                    .toList();

            BehaviorContext behaviorContext = BehaviorContext.withCandidates(
                    snapshot,
                    regionId,
                    false, // shared food not yet resolved in M4
                    false, // shared medicine not yet resolved in M4
                    candidates
            );

            Action action = actionDecider.apply(behaviorContext);

            ActionContext actionContext = new ActionContext(
                    snapshot,
                    regionId,
                    false,
                    false
            );

            ActionResult result = action.evaluate(actionContext);

            localActionSequence++;
            survivorResults.add(new SurvivorActionResult(survivorId, localActionSequence, result));

            final int actionSeq = localActionSequence;
            result.migrationRequest().ifPresent(dest -> {
                migrationRequests.add(new MigrationRequest(survivorId, regionId, dest, actionSeq));
            });
        }

        return new RegionResult(regionId, List.copyOf(survivorResults), List.copyOf(migrationRequests));
    }
}
