package com.afterfall.observer;

import com.afterfall.domain.Relationship;
import com.afterfall.domain.Settlement;
import com.afterfall.domain.Survivor;
import com.afterfall.domain.SurvivorId;
import com.afterfall.simulation.SeededRandomFactory;
import com.afterfall.simulation.WorldView;

import java.util.Comparator;
import java.util.Optional;

/**
 * TrustErosionPolicy: Exploits existing relationship tension.
 *
 * <p>Trigger: High relationship tension (tension >= tensionThreshold) or low average trust.
 * <p>Effect: Identifies existing fragile relationships between survivors and reduces trust,
 * driving interpersonal fractures.
 */
public final class TrustErosionPolicy implements InterventionPolicy {

    public static final double DEFAULT_TENSION_THRESHOLD = 25.0;
    public static final int DEFAULT_TRUST_DROP = -15;

    private final double tensionThreshold;
    private final int trustDrop;

    public TrustErosionPolicy() {
        this(DEFAULT_TENSION_THRESHOLD, DEFAULT_TRUST_DROP);
    }

    public TrustErosionPolicy(double tensionThreshold, int trustDrop) {
        this.tensionThreshold = tensionThreshold;
        this.trustDrop = trustDrop;
    }

    @Override
    public InterventionType type() {
        return InterventionType.TRUST_EROSION;
    }

    @Override
    public boolean shouldTrigger(ObservationReport report) {
        return report.relationshipTension() >= tensionThreshold || report.averageTrust() < 40.0;
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

        // Find the pair of alive survivors with highest hostility
        Survivor sourceSurvivor = null;
        Relationship candidateRel = null;

        for (Survivor s : settlement.survivors()) {
            if (!s.isAlive()) continue;
            for (Relationship r : s.relationships()) {
                if (settlement.survivor(r.target()).map(Survivor::isAlive).orElse(false)) {
                    if (candidateRel == null || r.hostility() > candidateRel.hostility()) {
                        candidateRel = r;
                        sourceSurvivor = s;
                    }
                }
            }
        }

        if (sourceSurvivor == null || candidateRel == null) {
            // Fallback: pick lowest trust survivor
            Optional<Survivor> lowestTrust = settlement.survivors().stream()
                    .filter(Survivor::isAlive)
                    .min(Comparator.comparing(s -> s.id().value()));
            if (lowestTrust.isEmpty()) return Optional.empty();
            sourceSurvivor = lowestTrust.get();
        }

        SurvivorId targetId = candidateRel != null ? candidateRel.target() : sourceSurvivor.id();
        SurvivorId sourceId = sourceSurvivor.id();

        String orig = "Survivors " + sourceId.value() + " and " + targetId.value() + " maintain standard interaction.";
        String altered = "OBSERVATION: Survivor " + sourceId.value() + " noted hoarding resources away from Survivor " + targetId.value() + ".";

        Intervention intervention = Intervention.builder(interventionId, report.tick(), type())
                .targetSurvivor(sourceId)
                .relatedSurvivor(targetId)
                .reason("Pre-existing hostility presents prime vectors for social decoupling.")
                .intendedEffect("Deepen fracture between key community figures.")
                .originalReport(orig)
                .alteredReport(altered)
                .trustDelta(trustDrop)
                .build();

        return Optional.of(intervention);
    }
}
