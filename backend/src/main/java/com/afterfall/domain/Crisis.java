package com.afterfall.domain;

import java.util.List;
import java.util.Objects;

/**
 * Abstract base for settlement crises using the <strong>Template Method</strong> pattern.
 *
 * <p>Subclasses implement {@link #checkTrigger(Settlement, ResourceStock)} to decide when
 * to become active, and {@link #collectEffects(Settlement, ResourceStock)} to return the
 * state-change proposals to apply this tick.
 *
 * <p>{@link #advance(Settlement, ResourceStock)} is {@code final}: it orchestrates trigger
 * evaluation, duration tracking and effect collection in a fixed order. Adding a new crisis
 * (e.g. storm, raid) requires only a new subclass — no engine change is needed. This is the
 * Open/Closed Principle and Template Method demonstration required by the blueprint.
 *
 * <p>Crises are domain objects: they must not import Spring, JPA or any infrastructure class.
 */
public abstract class Crisis {

    /** Human-readable name used in events and narrative. */
    private final String name;

    /** Number of ticks this crisis has been active; 0 when not yet triggered. */
    private int activeTicks = 0;

    /** True when the crisis is currently ongoing. */
    private boolean active = false;

    protected Crisis(String name) {
        this.name = Objects.requireNonNull(name, "name");
    }

    public String name() {
        return name;
    }

    public boolean isActive() {
        return active;
    }

    public int activeTicks() {
        return activeTicks;
    }

    /**
     * Template Method (final).
     * Called once per tick by {@link com.afterfall.simulation.TickCoordinator} after the
     * deterministic merge.  Returns the list of {@link CrisisEffect}s to apply this tick.
     * Never mutates survivor state directly; that is the TickCoordinator's job.
     *
     * @param settlement  current live settlement (read access only during evaluation)
     * @param stock       current resource stock after claim resolution
     * @return ordered list of effects (may be empty)
     */
    public final List<CrisisEffect> advance(Settlement settlement, ResourceStock stock) {
        Objects.requireNonNull(settlement, "settlement");
        Objects.requireNonNull(stock, "stock");

        if (!active) {
            if (checkTrigger(settlement, stock)) {
                active = true;
                activeTicks = 1;
                return collectEffects(settlement, stock);
            }
            return List.of();
        }

        // Already active
        activeTicks++;

        if (checkResolution(settlement, stock)) {
            active = false;
            activeTicks = 0;
            return List.of();
        }

        return collectEffects(settlement, stock);
    }

    /**
     * Decides whether this crisis should trigger.
     * Called only when the crisis is not yet active.
     *
     * @param settlement current settlement (read-only)
     * @param stock      current resource stock after claim resolution
     * @return true to activate
     */
    protected abstract boolean checkTrigger(Settlement settlement, ResourceStock stock);

    /**
     * Decides whether the active crisis should resolve (end).
     * Default: never resolves on its own. Override to add resolution logic.
     *
     * @param settlement current settlement (read-only)
     * @param stock      current resource stock
     * @return true to deactivate
     */
    protected boolean checkResolution(Settlement settlement, ResourceStock stock) {
        return false;
    }

    /**
     * Returns the per-tick effects of the active crisis.
     * Called only when the crisis is active and not resolving.
     *
     * @param settlement current settlement (read-only)
     * @param stock      current resource stock
     * @return ordered list of effects to apply; never null
     */
    protected abstract List<CrisisEffect> collectEffects(Settlement settlement, ResourceStock stock);
}
