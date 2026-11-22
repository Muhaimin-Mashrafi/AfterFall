package com.afterfall.simulation;

import com.afterfall.action.ActionResult;
import com.afterfall.domain.*;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Applies an {@link ActionResult} to a {@link Survivor} atomically.
 * Provides both a single‑survivor API (retain for existing callers) and a batch API
 * guaranteeing all‑or‑nothing semantics across a deterministic ordered set of survivors.
 */
public final class StateChangeApplier {

    private StateChangeApplier() {}

    /**
     * Apply a single {@link ActionResult} to the given survivor.
     * Rolls back any partial mutation if a domain operation throws.
     */
    public static void apply(ActionResult result, Survivor survivor) {
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(survivor, "survivor");
        if (!result.isSuccess()) {
            throw new IllegalArgumentException("Cannot apply a rejected ActionResult");
        }
        // Snapshot original state for rollback
        Needs needs = survivor.needs();
        Health health = survivor.health();
        Inventory inventory = survivor.inventory();
        Map<Need, Integer> origNeeds = new EnumMap<>(Need.class);
        for (Need n : Need.values()) {
            origNeeds.put(n, needs.level(n));
        }
        int origVitality = health.vitality();
        HealthCondition origCondition = health.condition();
        Map<ItemType, Integer> origInv = new EnumMap<>(ItemType.class);
        for (ItemType it : ItemType.values()) {
            origInv.put(it, inventory.quantity(it));
        }
        try {
            // apply deltas
            for (Map.Entry<Need, Integer> e : result.needDeltas().entrySet()) {
                needs.adjust(e.getKey(), e.getValue());
            }
            if (result.vitalityDelta() != 0) {
                health.adjustVitality(result.vitalityDelta());
            }
            result.newCondition().ifPresent(health::setCondition);
            for (Map.Entry<ItemType, Integer> e : result.inventoryDeltas().entrySet()) {
                int delta = e.getValue();
                ItemType type = e.getKey();
                if (delta > 0) {
                    inventory.add(type, delta);
                } else if (delta < 0) {
                    inventory.remove(type, -delta);
                }
            }
        } catch (RuntimeException ex) {
            // rollback
            for (Need n : Need.values()) {
                needs.set(n, origNeeds.get(n));
            }
            health.setVitality(origVitality);
            health.setCondition(origCondition);
            for (ItemType it : ItemType.values()) {
                int cur = inventory.quantity(it);
                int target = origInv.get(it);
                int diff = target - cur;
                if (diff > 0) {
                    inventory.add(it, diff);
                } else if (diff < 0) {
                    inventory.remove(it, -diff);
                }
            }
            throw ex;
        }
    }

    /**
     * Batch‑apply proposals to multiple survivors with deterministic ordering.
     * The {@code proposals} map must preserve iteration order (e.g., a {@link LinkedHashMap}).
     * All proposals are pre‑validated; if any validation fails, no mutation occurs.
     * If applying any proposal throws, every survivor is rolled back to its original state.
     *
     * @param proposals ordered map of survivor → action result
     * @throws IllegalArgumentException if any result is rejected or fails pre‑validation
     * @throws RuntimeException if an unexpected error occurs during application; survivors are restored.
     */
    public static void applyBatch(LinkedHashMap<Survivor, ActionResult> proposals) {
        applyBatch(proposals, StateChangeApplier::apply);
    }

    /**
     * Package-private batch-apply allowing custom mutation strategy for failure injection testing.
     * Production calls use {@link #apply(ActionResult, Survivor)}.
     */
    static void applyBatch(LinkedHashMap<Survivor, ActionResult> proposals, BiConsumer<ActionResult, Survivor> applierStrategy) {
        Objects.requireNonNull(proposals, "proposals");
        Objects.requireNonNull(applierStrategy, "applierStrategy");

        // Pre‑validation without side effects
        for (Map.Entry<Survivor, ActionResult> e : proposals.entrySet()) {
            Survivor s = Objects.requireNonNull(e.getKey(), "survivor key");
            ActionResult r = Objects.requireNonNull(e.getValue(), "action result");
            if (!r.isSuccess()) {
                throw new IllegalArgumentException("Rejected ActionResult in batch");
            }
            // Validate inventory deltas against current state
            Inventory inv = s.inventory();
            for (Map.Entry<ItemType, Integer> invDelta : r.inventoryDeltas().entrySet()) {
                int delta = invDelta.getValue();
                ItemType type = invDelta.getKey();
                if (delta < 0 && !inv.has(type, -delta)) {
                    throw new IllegalArgumentException("Insufficient inventory for removal in batch");
                }
                if (delta > 0 && inv.freeSpace() < delta) {
                    throw new IllegalArgumentException("Insufficient free space for addition in batch");
                }
            }
        }

        // Capture snapshots for all survivors
        class Snapshot {
            final Needs needs;
            final Health health;
            final Inventory inventory;
            final Map<Need, Integer> needLevels = new EnumMap<>(Need.class);
            final int vitality;
            final HealthCondition condition;
            final Map<ItemType, Integer> invQuant = new EnumMap<>(ItemType.class);
            Snapshot(Survivor s) {
                this.needs = s.needs();
                this.health = s.health();
                this.inventory = s.inventory();
                for (Need n : Need.values()) {
                    needLevels.put(n, needs.level(n));
                }
                this.vitality = health.vitality();
                this.condition = health.condition();
                for (ItemType it : ItemType.values()) {
                    invQuant.put(it, inventory.quantity(it));
                }
            }
        }
        Map<Survivor, Snapshot> snapshots = new LinkedHashMap<>();
        for (Survivor s : proposals.keySet()) {
            snapshots.put(s, new Snapshot(s));
        }

        // Apply sequentially using the specified strategy
        try {
            for (Map.Entry<Survivor, ActionResult> e : proposals.entrySet()) {
                applierStrategy.accept(e.getValue(), e.getKey());
            }
        } catch (RuntimeException applyEx) {
            // Roll back every survivor using its snapshot
            RuntimeException rollbackEx = null;
            for (Map.Entry<Survivor, Snapshot> entry : snapshots.entrySet()) {
                Survivor s = entry.getKey();
                Snapshot snap = entry.getValue();
                try {
                    // restore needs
                    for (Need n : Need.values()) {
                        snap.needs.set(n, snap.needLevels.get(n));
                    }
                    // restore health
                    snap.health.setVitality(snap.vitality);
                    snap.health.setCondition(snap.condition);
                    // restore inventory
                    for (ItemType it : ItemType.values()) {
                        int cur = snap.inventory.quantity(it);
                        int target = snap.invQuant.get(it);
                        int diff = target - cur;
                        if (diff > 0) {
                            snap.inventory.add(it, diff);
                        } else if (diff < 0) {
                            snap.inventory.remove(it, -diff);
                        }
                    }
                } catch (RuntimeException rb) {
                    if (rollbackEx == null) {
                        rollbackEx = rb;
                    } else {
                        rollbackEx.addSuppressed(rb);
                    }
                }
            }
            if (rollbackEx != null) {
                applyEx.addSuppressed(rollbackEx);
            }
            throw applyEx;
        }
    }
}
