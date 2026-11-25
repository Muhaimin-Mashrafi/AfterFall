package com.afterfall.choice;

import com.afterfall.domain.Need;
import com.afterfall.domain.ResourceType;
import com.afterfall.domain.SurvivorId;

import java.util.List;

/**
 * Authored choice moments raised by the simulation when a crisis begins. Pure factory: every
 * option and consequence is fixed text and numbers, so the same crisis tick always yields the
 * same moment.
 */
public final class LiveChoiceMoments {

    public static final String FAMINE_ID = "CHOICE_FAMINE";
    public static final String OUTBREAK_ID = "CHOICE_OUTBREAK";

    private LiveChoiceMoments() {
    }

    /** The stores are nearly empty. Targets are the living survivors at the time of the choice. */
    public static ChoiceMoment famine(long tick, String chapterId, List<SurvivorId> alive) {
        ChoiceOption equal = new ChoiceOption("EQUAL_RATIONS", "Share the stores equally",
                "Every household draws the same small ration.",
                ChoiceConsequence.builder("Equal rations were ordered, and the stores stretched a little further.")
                        .resourceDelta(ResourceType.FOOD, 8).trustDelta(3).targetSurvivors(alive).build(), true);
        ChoiceOption workers = new ChoiceOption("FEED_WORKERS", "Feed the workers first",
                "Those who farm and haul eat more; the rest wait.",
                ChoiceConsequence.builder("The workers were fed first. The others noticed.")
                        .resourceDelta(ResourceType.FOOD, 14).trustDelta(-8).needDelta(Need.SAFETY, -5)
                        .targetSurvivors(alive).build(), false);
        ChoiceOption advisory = new ChoiceOption("FOLLOW_ADVISORY", "Follow the Observer's advisory",
                "Adopt the emergency measures the Observer recommends.",
                ChoiceConsequence.builder("The Observer's advisory was followed. The reserves fell faster than anyone expected.")
                        .resourceDelta(ResourceType.FOOD, -5).trustDelta(-5).targetSurvivors(alive).build(), false);
        return new ChoiceMoment(FAMINE_ID, tick, chapterId, "The Last Stores",
                "The storehouse is nearly bare. How should what remains be shared?",
                List.of(equal, workers, advisory), "EQUAL_RATIONS");
    }

    /** Several survivors are ill. */
    public static ChoiceMoment outbreak(long tick, String chapterId, List<SurvivorId> alive) {
        ChoiceOption quarantine = new ChoiceOption("QUARANTINE", "Separate the sick",
                "The ill are kept apart from the rest.",
                ChoiceConsequence.builder("The sick were kept apart. The quarters grew quiet and uneasy.")
                        .needDelta(Need.SAFETY, 8).trustDelta(-2).targetSurvivors(alive).build(), true);
        ChoiceOption share = new ChoiceOption("SHARE_MEDICINE", "Spend the medicine now",
                "Treat as many as the stock allows.",
                ChoiceConsequence.builder("The medicine was spent freely. Fewer were left to treat later.")
                        .resourceDelta(ResourceType.MEDICINE, -6).trustDelta(5).targetSurvivors(alive).build(), false);
        ChoiceOption reserve = new ChoiceOption("KEEP_RESERVE", "Keep supplies for the healthy",
                "Hold back the remaining medicine.",
                ChoiceConsequence.builder("The remaining medicine was held back. Not everyone forgave it.")
                        .needDelta(Need.SAFETY, -8).trustDelta(-6).targetSurvivors(alive).build(), false);
        return new ChoiceMoment(OUTBREAK_ID, tick, chapterId, "The Sick Quarter",
                "Illness is spreading. What should the settlement do?",
                List.of(quarantine, share, reserve), "QUARANTINE");
    }
}
