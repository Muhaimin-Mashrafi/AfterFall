package com.afterfall.simulation;

import com.afterfall.domain.Crisis;
import com.afterfall.domain.CrisisEffect;
import com.afterfall.domain.DiseaseOutbreak;
import com.afterfall.domain.FamineCrisis;
import com.afterfall.domain.ResourceStock;
import com.afterfall.domain.Settlement;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Holds the two MUST crises and advances them each tick at the post-merge boundary.
 *
 * <p>The TickCoordinator calls {@link #advanceAll(Settlement, ResourceStock)} after resolving
 * resource claims. The returned {@link CrisisEffect}s are then applied by the coordinator
 * as the sole writer, keeping the simulation core deterministic.
 *
 * <p>This class is not thread-safe; it must only be accessed from within the
 * TickCoordinator's single-writer merge phase.
 */
public final class CrisisManager {

    private final List<Crisis> crises;

    /** Constructs a manager with the two MUST crises at their default thresholds. */
    public CrisisManager() {
        this(new FamineCrisis(), new DiseaseOutbreak());
    }

    /** Package-private constructor for testing with custom crisis instances. */
    CrisisManager(Crisis... crises) {
        Objects.requireNonNull(crises, "crises");
        this.crises = List.of(crises);
    }

    /**
     * Advances all managed crises and collects their effects for the current tick.
     *
     * @param settlement current live settlement (read-only during crisis evaluation)
     * @param stock      resource stock after claim resolution
     * @return ordered list of effects from all active crises; may be empty
     */
    public List<CrisisEffect> advanceAll(Settlement settlement, ResourceStock stock) {
        Objects.requireNonNull(settlement, "settlement");
        Objects.requireNonNull(stock, "stock");

        List<CrisisEffect> allEffects = new ArrayList<>();
        for (Crisis crisis : crises) {
            allEffects.addAll(crisis.advance(settlement, stock));
        }
        return allEffects;
    }

    /** Returns the list of managed crises (unmodifiable). */
    public List<Crisis> crises() {
        return crises;
    }

    /** Whether any crisis is currently active. */
    public boolean hasActiveCrisis() {
        return crises.stream().anyMatch(Crisis::isActive);
    }
}
