package com.afterfall.recap;

import java.util.List;
import java.util.Objects;

/**
 * The final documentary recording closing out a simulation run.
 *
 * <p>Requirements:
 * <ul>
 *   <li>Derived from actual persisted run history and captions.</li>
 *   <li>Follows the implied, non-graphic narrative register.</li>
 *   <li>Contains the mandatory exact closing line:
 *       <i>"The settlement fell silent. The Observer remains active."</i></li>
 * </ul>
 */
public record FinalRecording(
        String runId,
        long finalTick,
        String endingChapter,
        List<RecordingScene> scenes,
        String closingLine
) {
    public static final String MANDATORY_CLOSING_LINE = "The settlement fell silent. The Observer remains active.";

    public FinalRecording {
        Objects.requireNonNull(runId, "runId");
        Objects.requireNonNull(endingChapter, "endingChapter");
        scenes = List.copyOf(Objects.requireNonNull(scenes, "scenes"));
        Objects.requireNonNull(closingLine, "closingLine");
    }
}
