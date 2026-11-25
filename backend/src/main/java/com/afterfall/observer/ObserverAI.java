package com.afterfall.observer;

import com.afterfall.domain.Settlement;
import com.afterfall.simulation.SeededRandomFactory;
import com.afterfall.simulation.WorldView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * ObserverAI: The fictional AI antagonist operating under deterministic rules.
 *
 * <p>Requirements:
 * <ul>
 *   <li>Evaluates an immutable {@link WorldView} snapshot (and read-only settlement metrics).</li>
 *   <li>NEVER mutates Survivor or Settlement state directly.</li>
 *   <li>Evaluates the four MUST policies in priority order.</li>
 *   <li>Proposes {@link Intervention} commands to be queued into the simulation engine.</li>
 *   <li>Maintains an audit trail of {@link InterventionRecord}s (evidence archive).</li>
 * </ul>
 */
public final class ObserverAI {

    /** Minimum ticks between two interventions of the same type (4 ticks = 1 day). */
    public static final int COOLDOWN_TICKS = 4;

    private final List<InterventionPolicy> policies;
    private final List<InterventionRecord> records = new ArrayList<>();
    private final java.util.Map<InterventionType, Long> lastInterventionTickByType = new java.util.EnumMap<>(InterventionType.class);
    private long nextInterventionId = 1L;

    public ObserverAI() {
        this(List.of(
                new InformationDistortionPolicy(),
                new TrustErosionPolicy(),
                new ResourcePressurePolicy(),
                new SelectiveDisclosurePolicy()
        ));
    }

    public ObserverAI(List<InterventionPolicy> policies) {
        this.policies = List.copyOf(Objects.requireNonNull(policies, "policies"));
    }

    /**
     * Analyses the completed tick snapshot and proposes an intervention if threshold rules trigger.
     * Purely read-only; zero domain mutation occurs here.
     */
    public Optional<Intervention> analyzeAndPropose(
            WorldView worldView,
            Settlement settlement,
            SeededRandomFactory randomFactory
    ) {
        Objects.requireNonNull(worldView, "worldView");
        Objects.requireNonNull(settlement, "settlement");
        Objects.requireNonNull(randomFactory, "randomFactory");

        ObservationReport report = ObservationReport.from(worldView, settlement);

        for (InterventionPolicy policy : policies) {
            // Per-type cooldown: the same policy cannot fire again within COOLDOWN_TICKS (1 day).
            Long lastTick = lastInterventionTickByType.get(policy.type());
            if (lastTick != null && worldView.tick() - lastTick < COOLDOWN_TICKS) {
                continue;
            }

            if (policy.shouldTrigger(report)) {
                long id = nextInterventionId++;
                Optional<Intervention> proposed = policy.evaluate(report, worldView, settlement, id, randomFactory);
                if (proposed.isPresent()) {
                    lastInterventionTickByType.put(policy.type(), worldView.tick());
                    return proposed;
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Records the final result of an intervention (applied or rejected).
     */
    public void record(Intervention intervention, InterventionStatus status, String result) {
        records.add(InterventionRecord.from(intervention, status, result));
    }

    /**
     * Returns an unmodifiable view of all recorded intervention evidence.
     */
    public List<InterventionRecord> records() {
        return Collections.unmodifiableList(records);
    }

    /**
     * Returns the active policies.
     */
    public List<InterventionPolicy> policies() {
        return policies;
    }
}
