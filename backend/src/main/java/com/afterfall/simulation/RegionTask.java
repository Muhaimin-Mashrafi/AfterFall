package com.afterfall.simulation;

import com.afterfall.action.Action;
import com.afterfall.behavior.BehaviorContext;
import com.afterfall.domain.RegionId;

import java.util.Objects;
import java.util.SplittableRandom;
import java.util.concurrent.Callable;
import java.util.function.Function;

/**
 * Immutable task representing the evaluation of one region for a single simulation tick.
 * Submitted to the 4-thread region worker pool.
 */
public final class RegionTask implements Callable<RegionResult> {

    private final RegionId regionId;
    private final WorldView worldView;
    private final long tick;
    private final SplittableRandom rng;
    private final Function<BehaviorContext, Action> actionDecider;

    public RegionTask(RegionId regionId,
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

    public RegionId regionId() {
        return regionId;
    }

    public WorldView worldView() {
        return worldView;
    }

    public long tick() {
        return tick;
    }

    public SplittableRandom rng() {
        return rng;
    }

    public Function<BehaviorContext, Action> actionDecider() {
        return actionDecider;
    }

    @Override
    public RegionResult call() {
        RegionWorker worker = new RegionWorker(regionId, worldView, tick, rng, actionDecider);
        return worker.evaluate();
    }
}
