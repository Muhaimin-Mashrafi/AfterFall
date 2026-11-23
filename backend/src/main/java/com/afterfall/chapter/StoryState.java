package com.afterfall.chapter;

import com.afterfall.domain.Settlement;
import com.afterfall.domain.Survivor;
import com.afterfall.domain.Need;

import java.util.Objects;

/**
 * Immutable read-only projection of the simulation's socio-narrative state
 * evaluated by {@link ChapterPolicy} strategies.
 */
public record StoryState(
        long currentTick,
        ChapterType currentChapter,
        long chapterStartTick,
        int activePopulation,
        int averageTrust,
        int averageMorale,
        int consecutiveShortageTicks,
        int discoveredEvidenceCount,
        int observerExposureLevel,
        int visibleInterventionsCount,
        int consecutiveCriticalTicks
) {
    public StoryState {
        Objects.requireNonNull(currentChapter, "currentChapter");
    }

    public long chapterDwell() {
        return currentTick - chapterStartTick;
    }

    public static StoryState snapshot(Settlement settlement,
                                       ChapterType currentChapter,
                                       long chapterStartTick,
                                       long currentTick,
                                       int shortageTicks,
                                       int evidenceCount,
                                       int exposureLevel,
                                       int visibleInterventions,
                                       int criticalTicks) {
        int aliveCount = 0;
        int totalMorale = 0;
        int totalTrust = 0;
        int trustPairCount = 0;

        for (Survivor s : settlement.survivors()) {
            if (s.isAlive()) {
                aliveCount++;
                int safety = s.needs().level(Need.SAFETY);
                int rest = s.needs().level(Need.REST);
                int food = s.needs().level(Need.FOOD);
                int water = s.needs().level(Need.WATER);
                totalMorale += (safety + rest + food + water) / 4;

                for (var rel : s.relationships()) {
                    totalTrust += rel.trust();
                    trustPairCount++;
                }
            }
        }

        int avgMorale = aliveCount > 0 ? totalMorale / aliveCount : 0;
        int avgTrust = trustPairCount > 0 ? totalTrust / trustPairCount : 50;

        return new StoryState(
                currentTick,
                currentChapter,
                chapterStartTick,
                aliveCount,
                avgTrust,
                avgMorale,
                shortageTicks,
                evidenceCount,
                exposureLevel,
                visibleInterventions,
                criticalTicks
        );
    }
}
