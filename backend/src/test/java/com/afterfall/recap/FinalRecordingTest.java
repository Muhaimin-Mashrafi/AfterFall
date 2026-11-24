package com.afterfall.recap;

import com.afterfall.application.persistence.RunId;
import com.afterfall.application.persistence.RunMetadata;
import com.afterfall.application.persistence.RunRecapInputs;
import com.afterfall.chapter.ChapterTransition;
import com.afterfall.chapter.ChapterType;
import com.afterfall.domain.EventId;
import com.afterfall.domain.SurvivorId;
import com.afterfall.events.EventSeverity;
import com.afterfall.narrator.NarrativeTemplateRepository;
import com.afterfall.narrator.StoryCaption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class FinalRecordingTest {

    private final FinalRecordingGenerator generator = new FinalRecordingGenerator();

    @Test
    @DisplayName("Mandatory exact closing line is present")
    void testMandatoryClosingLine() {
        RunRecapInputs inputs = new RunRecapInputs(
                RunId.of("test-run"),
                new RunMetadata(RunId.of("test-run"), 42L, 25, Instant.now(), null, "CHAPTER_4_THE_LAST_RECORDING", "COMPLETED"),
                List.of(new ChapterTransition(20L, ChapterType.CHAPTER_3_THE_REVELATION, ChapterType.CHAPTER_4_THE_LAST_RECORDING, "Threshold reached", false)),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );

        FinalRecording recording = generator.generate(inputs);
        assertNotNull(recording);
        assertEquals("The settlement fell silent. The Observer remains active.", recording.closingLine());
        assertEquals(FinalRecording.MANDATORY_CLOSING_LINE, recording.closingLine());
    }

    @Test
    @DisplayName("Banned-term compliance check passes on generated scenes")
    void testBannedTermCompliance() {
        RunRecapInputs inputs = new RunRecapInputs(
                RunId.of("test-run"),
                new RunMetadata(RunId.of("test-run"), 42L, 25, Instant.now(), null, "CHAPTER_4_THE_LAST_RECORDING", "COMPLETED"),
                List.of(new ChapterTransition(10L, ChapterType.CHAPTER_1_A_NEW_BEGINNING, ChapterType.CHAPTER_2_THE_FRACTURE, "Trust decline", false)),
                Collections.emptyList(),
                Collections.emptyList(),
                List.of(
                        new StoryCaption(1L, 11L, "Day 3. The east shelter stood vacant by morning.", EventSeverity.CRITICAL, List.of(new EventId(1L)), List.of(new SurvivorId(1))),
                        new StoryCaption(2L, 12L, "Her chair stayed turned toward the window; no words were spoken.", EventSeverity.CRITICAL, List.of(new EventId(2L)), List.of(new SurvivorId(2)))
                ),
                Collections.emptyList()
        );

        FinalRecording recording = generator.generate(inputs);
        assertFalse(recording.scenes().isEmpty());

        for (RecordingScene s : recording.scenes()) {
            String text = s.narrativeText().toLowerCase(Locale.ROOT);
            for (String banned : NarrativeTemplateRepository.BANNED_TERMS) {
                assertFalse(text.contains(banned), "Scene contains prohibited term: " + banned);
            }
        }
    }
}
