package com.afterfall.observer;

import com.afterfall.domain.HealthCondition;
import com.afterfall.domain.Need;
import com.afterfall.domain.Settlement;
import com.afterfall.simulation.SeededRandomFactory;
import com.afterfall.simulation.SurvivorView;
import com.afterfall.simulation.WorldView;

import java.util.Comparator;
import java.util.Optional;

/**
 * SelectiveDisclosurePolicy: Reveals some facts and withholds others.
 *
 * <p>Trigger: Outbreaks or health vulnerabilities present (illSurvivors >= threshold or avgMorale < threshold).
 * <p>Effect: Discloses critical contagion warnings while withholding medical availability,
 * triggering panic and isolating ill survivors.
 */
public final class SelectiveDisclosurePolicy implements InterventionPolicy {

    public static final int DEFAULT_ILL_TRIGGER = 2;
    public static final double DEFAULT_LOW_MORALE = 45.0;

    private final int illTriggerThreshold;
    private final double lowMoraleThreshold;

    public SelectiveDisclosurePolicy() {
        this(DEFAULT_ILL_TRIGGER, DEFAULT_LOW_MORALE);
    }

    public SelectiveDisclosurePolicy(int illTriggerThreshold, double lowMoraleThreshold) {
        this.illTriggerThreshold = illTriggerThreshold;
        this.lowMoraleThreshold = lowMoraleThreshold;
    }

    @Override
    public InterventionType type() {
        return InterventionType.SELECTIVE_DISCLOSURE;
    }

    @Override
    public boolean shouldTrigger(ObservationReport report) {
        return report.illSurvivors() >= illTriggerThreshold || report.averageMorale() < lowMoraleThreshold;
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

        // Find an ill survivor or the most vulnerable survivor
        Optional<SurvivorView> illSurvivor = worldView.survivors().values().stream()
                .filter(s -> s.status().name().equals("ALIVE") && s.condition() == HealthCondition.ILL)
                .findFirst();

        SurvivorView target = illSurvivor.orElseGet(() ->
                worldView.survivors().values().stream()
                        .filter(s -> s.status().name().equals("ALIVE"))
                        .min(Comparator.comparingInt(SurvivorView::vitality))
                        .orElse(null)
        );

        if (target == null) return Optional.empty();

        String orig = "Health report: " + report.illSurvivors() + " infected. Medicine stock: " + report.medicineStock() + " units available.";
        String altered = "SANITARY ALERT: Uncontrolled pathogen spreading via Survivor #" + target.id().value() + ". Quarantine unverified.";

        Intervention intervention = Intervention.builder(interventionId, report.tick(), type())
                .targetSurvivor(target.id())
                .reason("Elevated contagion warrants selective fact dissemination to guide isolation.")
                .intendedEffect("Isolate vulnerable vector while omitting cure availability.")
                .originalReport(orig)
                .alteredReport(altered)
                .needDelta(Need.SAFETY, -20)
                .build();

        return Optional.of(intervention);
    }
}
