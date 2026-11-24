package com.afterfall.recap;

import com.afterfall.application.persistence.RunRecapInputs;
import com.afterfall.chapter.ChapterTransition;
import com.afterfall.narrator.StoryCaption;
import com.afterfall.narrator.NarrativeTemplateRepository;

import java.util.*;

/**
 * Domain generator for {@link FinalRecording}.
 *
 * <p>Requirements:
 * <ul>
 *   <li>Generated exclusively from persisted run history and story captions.</li>
 *   <li>Preserves chronological order.</li>
 *   <li>Maintains source EventId and SurvivorId links.</li>
 *   <li>Enforces the restrained, implied, non-graphic narrative register.</li>
 *   <li>Enforces the mandatory exact closing line:
 *       <i>"The settlement fell silent. The Observer remains active."</i></li>
 *   <li>Rejects any banned terms.</li>
 * </ul>
 */
public class FinalRecordingGenerator {

    /**
     * Synthesizes the Final Recording from immutable recap inputs.
     */
    public FinalRecording generate(RunRecapInputs inputs) {
        Objects.requireNonNull(inputs, "inputs");

        String runIdStr = inputs.runId().value();
        String endingChapter = inputs.metadata() != null ? inputs.metadata().endingChapter() : "CHAPTER_4_THE_LAST_RECORDING";
        long finalTick = 0L;

        List<RecordingScene> scenes = new ArrayList<>();

        // 1. Gather milestone scenes from chapter transitions
        for (ChapterTransition ct : inputs.chapterTransitions()) {
            finalTick = Math.max(finalTick, ct.tick());
            String text = "Day " + (ct.tick() / 4 + 1) + ". Transition toward " + ct.toChapter().displayName() + ": " + ct.triggerReason();
            validateNoBannedTerms(text);
            scenes.add(new RecordingScene(ct.tick(), ct.toChapter().name(), text, Collections.emptyList(), Collections.emptyList()));
        }

        // 2. Select significant narrative captions (captions with priority or marking turning points)
        for (StoryCaption c : inputs.captions()) {
            finalTick = Math.max(finalTick, c.tick());
            // Filter to significant or critical captions for the documentary summary
            if (c.severity() == com.afterfall.events.EventSeverity.CRITICAL ||
                c.severity() == com.afterfall.events.EventSeverity.SIGNIFICANT ||
                scenes.size() < 12) {
                validateNoBannedTerms(c.text());
                scenes.add(new RecordingScene(
                        c.tick(),
                        endingChapter,
                        c.text(),
                        c.sourceEventIds(),
                        c.referencedSurvivorIds()
                ));
            }
        }

        // Ensure chronological order
        scenes.sort(Comparator.comparingLong(RecordingScene::tick));

        // If no captions or scenes exist (e.g. empty tick 0 run), provide an initial documentary scene
        if (scenes.isEmpty()) {
            scenes.add(new RecordingScene(
                    0L,
                    "CHAPTER_1_A_NEW_BEGINNING",
                    "The observation experiment commenced under quiet surveillance.",
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
        }

        return new FinalRecording(
                runIdStr,
                finalTick,
                endingChapter,
                scenes,
                FinalRecording.MANDATORY_CLOSING_LINE
        );
    }

    private void validateNoBannedTerms(String text) {
        if (text == null) return;
        String lower = text.toLowerCase(Locale.ROOT);
        for (String banned : NarrativeTemplateRepository.BANNED_TERMS) {
            if (lower.contains(banned)) {
                throw new IllegalStateException("Final recording violated non-graphic register with banned term: " + banned);
            }
        }
    }
}
