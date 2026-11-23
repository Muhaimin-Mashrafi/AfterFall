package com.afterfall.observer;

import com.afterfall.domain.Settlement;
import com.afterfall.simulation.SeededRandomFactory;
import com.afterfall.simulation.WorldView;

import java.util.Optional;

/**
 * Strategy pattern interface for ObserverAI intervention policies.
 */
public interface InterventionPolicy {

    /** The type of intervention this policy produces. */
    InterventionType type();

    /** Checks whether the policy trigger conditions are met by the observation report. */
    boolean shouldTrigger(ObservationReport report);

    /**
     * Evaluates the policy against the immutable world view and generates an immutable
     * Intervention command if appropriate.
     */
    Optional<Intervention> evaluate(
            ObservationReport report,
            WorldView worldView,
            Settlement settlement,
            long interventionId,
            SeededRandomFactory randomFactory
    );
}
