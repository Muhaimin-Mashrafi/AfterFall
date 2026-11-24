package com.afterfall.recap;

import com.afterfall.domain.EventId;
import com.afterfall.domain.SurvivorId;

import java.util.List;
import java.util.Objects;

/**
 * Immutable documentary scene within the Final Recording.
 *
 * <p>All scenes maintain source EventId and SurvivorId links and follow
 * the implied, non-graphic narrative register.
 */
public record RecordingScene(
        long tick,
        String chapter,
        String narrativeText,
        List<EventId> sourceEventIds,
        List<SurvivorId> referencedSurvivorIds
) {
    public RecordingScene {
        Objects.requireNonNull(chapter, "chapter");
        Objects.requireNonNull(narrativeText, "narrativeText");
        sourceEventIds = List.copyOf(Objects.requireNonNull(sourceEventIds, "sourceEventIds"));
        referencedSurvivorIds = List.copyOf(Objects.requireNonNull(referencedSurvivorIds, "referencedSurvivorIds"));
    }
}
