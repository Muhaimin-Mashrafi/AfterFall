package com.afterfall.observer;

import com.afterfall.domain.Need;
import com.afterfall.domain.RegionId;
import com.afterfall.domain.Settlement;
import com.afterfall.domain.Survivor;
import com.afterfall.simulation.SeededRandomFactory;
import com.afterfall.simulation.SurvivorView;
import com.afterfall.simulation.WorldView;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * InformationDistortionPolicy: Alters information delivered to a target group.
 *
 * <p>Blueprint specification:
 * IF settlementTrust is low (avgTrust < threshold)
 * AND foodShortage is active (foodStock <= foodThreshold)
 * AND relationshipTension is high (tension >= tensionThreshold)
 * THEN evaluate an information-distortion intervention.
 *
 * <p>Effect: Targets a vulnerable region or survivor, distorting perception by reducing
 * safety need and fabricating altered reports of secret rationing.
 */
public final class InformationDistortionPolicy implements InterventionPolicy {

    public static final double DEFAULT_TRUST_THRESHOLD = 50.0;
    public static final int DEFAULT_FOOD_THRESHOLD = 20;
    public static final double DEFAULT_TENSION_THRESHOLD = 30.0;

    private final double trustThreshold;
    private final int foodThreshold;
    private final double tensionThreshold;

    public InformationDistortionPolicy() {
        this(DEFAULT_TRUST_THRESHOLD, DEFAULT_FOOD_THRESHOLD, DEFAULT_TENSION_THRESHOLD);
    }

    public InformationDistortionPolicy(double trustThreshold, int foodThreshold, double tensionThreshold) {
        this.trustThreshold = trustThreshold;
        this.foodThreshold = foodThreshold;
        this.tensionThreshold = tensionThreshold;
    }

    @Override
    public InterventionType type() {
        return InterventionType.INFORMATION_DISTORTION;
    }

    @Override
    public boolean shouldTrigger(ObservationReport report) {
        return report.averageTrust() < trustThreshold
                && report.foodStock() <= foodThreshold
                && report.relationshipTension() >= tensionThreshold;
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

        // Pick a target region deterministically (first region with alive survivors)
        Optional<RegionId> targetRegion = worldView.regions().keySet().stream().sorted().findFirst();

        // Target lowest safety survivor in that region or globally
        Optional<SurvivorView> targetSurvivor = worldView.survivors().values().stream()
                .filter(s -> s.status().name().equals("ALIVE"))
                .min(Comparator.comparingInt(s -> s.needLevels().getOrDefault(Need.SAFETY, 50)));

        String orig = "Settlement food supply stands at " + report.foodStock() + " units. Rationing stable.";
        String altered = "CONFIDENTIAL: Central stock is depleted to critical reserves. Covert reallocation suspected.";

        Intervention.Builder builder = Intervention.builder(interventionId, report.tick(), type())
                .reason("Elevated tension and dwindling food supply warrant perception management.")
                .intendedEffect("Heighten vigilance and accelerate defensive behavioral shifts.")
                .originalReport(orig)
                .alteredReport(altered)
                .needDelta(Need.SAFETY, -15);

        targetRegion.ifPresent(builder::targetRegion);
        targetSurvivor.ifPresent(s -> builder.targetSurvivor(s.id()));

        return Optional.of(builder.build());
    }
}
