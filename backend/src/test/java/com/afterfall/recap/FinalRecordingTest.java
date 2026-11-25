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

    @Test
    @DisplayName("Duplicate transitions collapse; choices and Observer records become scenes; ending chapter is the last transition")
    void testStoryBeatsAndEndingChapter() {
        ChapterTransition t1 = new ChapterTransition(48L, ChapterType.CHAPTER_1_A_NEW_BEGINNING, ChapterType.CHAPTER_2_THE_FRACTURE, "Hard backstop triggered: x", true);
        ChapterTransition t2 = new ChapterTransition(96L, ChapterType.CHAPTER_2_THE_FRACTURE, ChapterType.CHAPTER_3_THE_REVELATION, "Evidence surfaced", false);
        List<ChapterTransition> transitions = new java.util.ArrayList<>();
        for (int i = 0; i < 50; i++) { transitions.add(t1); transitions.add(t2); }
        List<StoryCaption> captions = new java.util.ArrayList<>();
        for (int i = 0; i < 40; i++) {
            captions.add(new StoryCaption(i, 100 + i, "A quiet watch number " + i + ".", EventSeverity.MINOR, List.of(new EventId(i + 1)), List.of()));
        }
        captions.add(new StoryCaption(99, 120L, "The east shelter stood vacant by morning.", EventSeverity.CRITICAL, List.of(new EventId(99)), List.of(new SurvivorId(3))));
        captions.add(new StoryCaption(100, 96L, "Chapter III: The Revelation began: Evidence surfaced", EventSeverity.CRITICAL, List.of(), List.of()));

        RunRecapInputs inputs = new RunRecapInputs(
                RunId.of("r"),
                new RunMetadata(RunId.of("r"), 1L, 25, Instant.now(), null, "CHAPTER_1_A_NEW_BEGINNING", "RUNNING"),
                transitions,
                List.of(new com.afterfall.choice.ChoiceRecord("c1", 60L, "CHAPTER_2_THE_FRACTURE", "o1", "Share the stores", "Trust rose", false)),
                List.of(new com.afterfall.observer.InterventionRecord(1L, 70L, com.afterfall.observer.InterventionType.TRUST_EROSION, "Survivor #2",
                        "r", "i", "a", com.afterfall.observer.InterventionStatus.APPLIED, "o", "a")),
                captions,
                Collections.emptyList());

        FinalRecording rec = generator.generate(inputs);

        assertEquals("CHAPTER_3_THE_REVELATION", rec.endingChapter());
        assertTrue(rec.scenes().size() <= FinalRecordingGenerator.MAX_SCENES);
        assertEquals(rec.scenes().size(), rec.scenes().stream().map(RecordingScene::narrativeText).distinct().count());
        assertEquals(2, rec.scenes().stream().filter(s -> s.narrativeText().contains("began")).count());
        assertTrue(rec.scenes().stream().anyMatch(s -> s.narrativeText().contains("Share the stores") && s.chapter().equals("CHAPTER_2_THE_FRACTURE")));
        assertTrue(rec.scenes().stream().anyMatch(s -> s.narrativeText().startsWith("Day 18. Observer record:")));
        assertTrue(rec.scenes().stream().anyMatch(s -> s.narrativeText().contains("vacant by morning") && !s.sourceEventIds().isEmpty()));
        assertTrue(rec.scenes().stream().noneMatch(s -> s.narrativeText().contains("Hard backstop")));
        assertEquals(FinalRecording.MANDATORY_CLOSING_LINE, rec.closingLine());
    }
}
