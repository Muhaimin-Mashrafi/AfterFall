package com.afterfall.observer;

import com.afterfall.domain.HealthCondition;
import com.afterfall.domain.Need;
import com.afterfall.domain.ResourceType;
import com.afterfall.domain.Settlement;
import com.afterfall.domain.Survivor;
import com.afterfall.simulation.SurvivorView;
import com.afterfall.simulation.WorldView;

import java.util.Collection;
import java.util.Objects;

/**
 * Immutable aggregated metrics analyzed by ObserverAI to decide intervention triggers.
 *
 * <p>Computed directly from an immutable {@link WorldView} snapshot (or Settlement context),
 * ensuring thread-safety and deterministic evaluation.
 */
public record ObservationReport(
        long tick,
        int activeSurvivors,
        int illSurvivors,
        int foodStock,
        int waterStock,
        int medicineStock,
        double averageTrust,
        double averageMorale,
        double relationshipTension
) {
    public ObservationReport {
        if (tick < 0) throw new IllegalArgumentException("tick must not be negative");
        if (activeSurvivors < 0) throw new IllegalArgumentException("activeSurvivors must not be negative");
    }

    /**
     * Derives an observation report from the immutable WorldView and live Settlement relationships.
     */
    public static ObservationReport from(WorldView worldView, Settlement settlement) {
        Objects.requireNonNull(worldView, "worldView");
        Objects.requireNonNull(settlement, "settlement");

        int active = 0;
        int ill = 0;
        double totalSafety = 0;
        double totalRest = 0;

        for (SurvivorView sv : worldView.survivors().values()) {
            if (sv.status().name().equals("ALIVE")) {
                active++;
                if (sv.condition() == HealthCondition.ILL) {
                    ill++;
                }
                totalSafety += sv.needLevels().getOrDefault(Need.SAFETY, 50);
                totalRest += sv.needLevels().getOrDefault(Need.REST, 50);
            }
        }

        double avgMorale = active > 0 ? (totalSafety + totalRest) / (active * 2.0) : 0.0;

        // Aggregate relationships from settlement survivors
        int relCount = 0;
        double totalTrust = 0;
        double totalHostility = 0;

        for (Survivor s : settlement.survivors()) {
            if (s.isAlive()) {
                for (var rel : s.relationships()) {
                    relCount++;
                    totalTrust += rel.trust();
                    totalHostility += rel.hostility();
                }
            }
        }

        double avgTrust = relCount > 0 ? totalTrust / relCount : 50.0;
        double tension = relCount > 0 ? totalHostility / relCount : 0.0;

        int food = worldView.resourceQuantities().getOrDefault(ResourceType.FOOD, 0);
        int water = worldView.resourceQuantities().getOrDefault(ResourceType.WATER, 0);
        int medicine = worldView.resourceQuantities().getOrDefault(ResourceType.MEDICINE, 0);

        return new ObservationReport(
                worldView.tick(),
                active,
                ill,
                food,
                water,
                medicine,
                avgTrust,
                avgMorale,
                tension
        );
    }
}
