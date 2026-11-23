package com.afterfall.observer;

import com.afterfall.domain.Need;
import com.afterfall.domain.RegionId;
import com.afterfall.domain.Settlement;
import com.afterfall.simulation.SeededRandomFactory;
import com.afterfall.simulation.WorldView;

import java.util.Optional;

/**
 * ResourcePressurePolicy: Shapes perceptions of scarcity.
 *
 * <p>Trigger: Resource levels (food or medicine) falling below alert thresholds.
 * <p>Effect: Amplifies psychological hunger/exhaustion, reducing food and rest needs
 * to induce competition.
 */
public final class ResourcePressurePolicy implements InterventionPolicy {

    public static final int DEFAULT_FOOD_ALERT = 30;
    public static final int DEFAULT_MEDICINE_ALERT = 15;

    private final int foodAlertThreshold;
    private final int medicineAlertThreshold;

    public ResourcePressurePolicy() {
        this(DEFAULT_FOOD_ALERT, DEFAULT_MEDICINE_ALERT);
    }

    public ResourcePressurePolicy(int foodAlertThreshold, int medicineAlertThreshold) {
        this.foodAlertThreshold = foodAlertThreshold;
        this.medicineAlertThreshold = medicineAlertThreshold;
    }

    @Override
    public InterventionType type() {
        return InterventionType.RESOURCE_PRESSURE;
    }

    @Override
    public boolean shouldTrigger(ObservationReport report) {
        return report.foodStock() <= foodAlertThreshold || report.medicineStock() <= medicineAlertThreshold;
    }

    @Override
    public Optional<Intervention> evaluate(ObservationReport report,
                                           WorldView worldView,
                                           Settlement settlement,
                                           long interventionId,
                                           SeededRandomFactory randomFactory) {
        if (!shouldTrigger(report)) {
            return Optional.empty();
        }

        RegionId targetRegion = worldView.regions().keySet().stream().sorted().findFirst()
                .orElse(RegionId.REGION_1);

        String orig = "Current reserves: Food=" + report.foodStock() + ", Meds=" + report.medicineStock() + ". Consumption steady.";
        String altered = "EMERGENCY ADVISORY: Stockpiles nearing irreversible depletion. Immediate rationing advised.";

        Intervention intervention = Intervention.builder(interventionId, report.tick(), type())
                .targetRegion(targetRegion)
                .reason("Resource reserves have entered warning thresholds.")
                .intendedEffect("Heighten perceived scarcity to induce preemptive competition.")
                .originalReport(orig)
                .alteredReport(altered)
                .needDelta(Need.FOOD, -10)
                .needDelta(Need.SAFETY, -10)
                .build();

        return Optional.of(intervention);
    }
}
