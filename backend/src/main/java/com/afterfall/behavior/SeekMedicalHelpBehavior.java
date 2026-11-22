package com.afterfall.behavior;

import com.afterfall.action.Action;
import com.afterfall.action.HealAction;
import com.afterfall.action.MoveAction;
import com.afterfall.domain.HealthCondition;
import com.afterfall.domain.ItemType;
import com.afterfall.domain.RegionId;
import java.util.Objects;
import java.util.Optional;

/**
 * Strategy for seeking medical assistance when ill, injured, or at low vitality.
 *
 * <p>Selects {@link HealAction} if medicine is available locally. If local medicine is unavailable
 * but nearby candidate regions exist, selects a {@link MoveAction} toward medical shelter.
 */
public final class SeekMedicalHelpBehavior implements Behavior {

    private final BehaviorConfig config;

    public SeekMedicalHelpBehavior() {
        this(BehaviorConfig.defaultConfig());
    }

    public SeekMedicalHelpBehavior(BehaviorConfig config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    @Override
    public Optional<Action> decide(BehaviorContext context) {
        Objects.requireNonNull(context, "context");

        if (!context.survivor().isAlive()) {
            return Optional.empty();
        }

        boolean warrantsMedicalAttention =
                context.survivor().condition() != HealthCondition.HEALTHY ||
                        context.survivor().vitality() <= config.healthVitalityThreshold();

        if (!warrantsMedicalAttention) {
            return Optional.empty();
        }

        boolean hasMedicineLocally =
                context.survivor().hasItem(ItemType.MEDICINE, 1) || context.medicineAvailable();

        if (hasMedicineLocally && context.survivor().condition() != HealthCondition.HEALTHY) {
            return Optional.of(new HealAction());
        }

        // If no local medicine, but candidate regions are provided, request movement to first valid candidate
        if (!context.candidateRegions().isEmpty()) {
            for (RegionId candidate : context.candidateRegions()) {
                if (!candidate.equals(context.currentRegion())) {
                    return Optional.of(new MoveAction(candidate));
                }
            }
        }

        return Optional.empty();
    }

    @Override
    public BehaviorType type() {
        return BehaviorType.SEEK_MEDICAL_HELP;
    }
}
