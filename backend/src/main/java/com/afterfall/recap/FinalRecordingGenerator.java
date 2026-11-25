package com.afterfall.recap;

import com.afterfall.application.persistence.RunRecapInputs;
import com.afterfall.chapter.ChapterTransition;
import com.afterfall.chapter.ChapterType;
import com.afterfall.choice.ChoiceRecord;
import com.afterfall.domain.EventId;
import com.afterfall.domain.SurvivorId;
import com.afterfall.events.EventSeverity;
import com.afterfall.observer.InterventionRecord;
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

    /** Upper bound on scenes so the recording stays a short documentary. */
    static final int MAX_SCENES = 14;
    /** Cap on Observer record scenes so they never crowd out deaths, crises and choices. */
    static final int MAX_OBSERVER_RECORDS = 3;

    /**
     * The chapter the run actually ended in: the destination of the latest persisted
     * chapter transition, else the recorded metadata chapter, else Chapter I.
     */
    public static String resolveEndingChapter(RunRecapInputs inputs) {
        List<ChapterTransition> transitions = new ArrayList<>(inputs.chapterTransitions());
        transitions.sort(Comparator.comparingLong(ChapterTransition::tick));
        if (!transitions.isEmpty()) {
            return transitions.get(transitions.size() - 1).toChapter().name();
        }
        if (inputs.metadata() != null && inputs.metadata().endingChapter() != null) {
            return inputs.metadata().endingChapter();
        }
        return ChapterType.CHAPTER_1_A_NEW_BEGINNING.name();
    }

    /**
     * Synthesizes the Final Recording from immutable recap inputs.
     *
     * <p>Scenes are real story beats: chapter changes, player choices, Observer records
     * and the most significant narrator captions. Duplicate transitions and captions are
     * collapsed, and each scene is labelled with the chapter in force at its tick.
     */
    public FinalRecording generate(RunRecapInputs inputs) {
        Objects.requireNonNull(inputs, "inputs");

        String runIdStr = inputs.runId().value();
        String endingChapter = resolveEndingChapter(inputs);

        List<ChapterTransition> transitions = distinctTransitions(inputs.chapterTransitions());
        long finalTick = 0L;

        // Mandatory beats: always kept.
        List<RecordingScene> mandatory = new ArrayList<>();
        for (ChapterTransition ct : transitions) {
            finalTick = Math.max(finalTick, ct.tick());
            mandatory.add(scene(ct.tick(), ct.toChapter().name(), transitionText(ct), List.of(), List.of()));
        }
        for (ChoiceRecord cr : inputs.choices()) {
            finalTick = Math.max(finalTick, cr.tick());
            String text = "Day " + day(cr.tick()) + ". A decision was recorded: " + cr.optionLabel()
                    + (cr.consequenceSummary() == null || cr.consequenceSummary().isBlank() ? "" : ". " + cr.consequenceSummary());
            mandatory.add(scene(cr.tick(), chapterAt(transitions, cr.tick()), text, List.of(), List.of()));
        }
        for (InterventionRecord ir : distinctInterventions(inputs.interventions())) {
            finalTick = Math.max(finalTick, ir.tick());
            // Observer claims are labelled so they stay distinguishable from simulation facts.
            String text = "Day " + day(ir.tick()) + ". Observer record: " + ir.type().name().toLowerCase(Locale.ROOT).replace('_', ' ')
                    + " concerning " + ir.targetDescription() + ".";
            mandatory.add(scene(ir.tick(), chapterAt(transitions, ir.tick()), text, List.of(), List.of()));
        }

        // Narrator captions: distinct text only, most severe first, thinned evenly over time.
        List<StoryCaption> captions = distinctCaptions(inputs.captions(), transitions);
        for (StoryCaption c : captions) {
            finalTick = Math.max(finalTick, c.tick());
        }
        int budget = Math.max(0, MAX_SCENES - mandatory.size());
        List<StoryCaption> chosen = new ArrayList<>();
        if (budget > 0) {
            List<StoryCaption> critical = captions.stream().filter(c -> c.severity() == EventSeverity.CRITICAL).toList();
            List<StoryCaption> significant = captions.stream().filter(c -> c.severity() == EventSeverity.SIGNIFICANT).toList();
            List<StoryCaption> minor = captions.stream().filter(c -> c.severity() == EventSeverity.MINOR).toList();
            Set<String> usedKeys = new HashSet<>();
            Set<SurvivorId> usedSurvivors = new HashSet<>();
            for (List<StoryCaption> tier : List.of(critical, significant, minor)) {
                int room = budget - chosen.size();
                if (room <= 0) break;
                // One beat per survivor and one per generic line, so no name or line repeats.
                List<StoryCaption> eligible = new ArrayList<>();
                Set<String> tierKeys = new HashSet<>(usedKeys);
                Set<SurvivorId> tierSurvivors = new HashSet<>(usedSurvivors);
                for (StoryCaption c : tier) {
                    List<SurvivorId> refs = c.referencedSurvivorIds();
                    boolean survivorSeen = tierSurvivors.stream().anyMatch(refs::contains);
                    if (survivorSeen || !tierKeys.add(genericKey(c.text()))) continue;
                    tierSurvivors.addAll(refs);
                    eligible.add(c);
                }
                List<StoryCaption> picked = thin(eligible, room);
                for (StoryCaption c : picked) {
                    usedKeys.add(genericKey(c.text()));
                    usedSurvivors.addAll(c.referencedSurvivorIds());
                }
                chosen.addAll(picked);
            }
        }

        List<RecordingScene> scenes = new ArrayList<>(mandatory);
        for (StoryCaption c : chosen) {
            scenes.add(scene(c.tick(), chapterAt(transitions, c.tick()), c.text(), c.sourceEventIds(), c.referencedSurvivorIds()));
        }
        // Stable sort keeps the deterministic insertion order for equal ticks.
        scenes.sort(Comparator.comparingLong(RecordingScene::tick));

        if (scenes.isEmpty()) {
            scenes.add(new RecordingScene(
                    0L,
                    ChapterType.CHAPTER_1_A_NEW_BEGINNING.name(),
                    "The observation experiment commenced under quiet surveillance.",
                    Collections.emptyList(),
                    Collections.emptyList()
            ));
        }

        return new FinalRecording(runIdStr, finalTick, endingChapter, scenes, FinalRecording.MANDATORY_CLOSING_LINE);
    }

    private RecordingScene scene(long tick, String chapter, String text, List<EventId> events, List<SurvivorId> survivors) {
        validateNoBannedTerms(text);
        return new RecordingScene(tick, chapter, text, events, survivors);
    }

    private static long day(long tick) {
        return tick / 4 + 1;
    }

    /** Plain-language transition line; mechanical trigger text (e.g. backstop) is not shown. */
    private static String transitionText(ChapterTransition ct) {
        String lead = "Day " + day(ct.tick()) + ". " + ct.toChapter().displayName() + " began";
        return ct.isForced() ? lead + " as the weeks wore on." : lead + ": " + ct.triggerReason();
    }

    /** Observer records are repetitive by nature: one per distinct (type, target), at most a few overall. */
    private static List<InterventionRecord> distinctInterventions(List<InterventionRecord> raw) {
        List<InterventionRecord> sorted = new ArrayList<>(raw);
        sorted.sort(Comparator.comparingLong(InterventionRecord::tick));
        Set<String> seen = new HashSet<>();
        List<InterventionRecord> distinct = new ArrayList<>();
        for (InterventionRecord ir : sorted) {
            if (seen.add(ir.type().name() + "|" + ir.targetDescription())) distinct.add(ir);
        }
        if (distinct.size() <= MAX_OBSERVER_RECORDS) return distinct;
        List<InterventionRecord> out = new ArrayList<>(MAX_OBSERVER_RECORDS);
        for (int i = 0; i < MAX_OBSERVER_RECORDS; i++) {
            out.add(distinct.get((int) ((long) i * distinct.size() / MAX_OBSERVER_RECORDS)));
        }
        return out;
    }

    private static List<ChapterTransition> distinctTransitions(List<ChapterTransition> raw) {
        List<ChapterTransition> sorted = new ArrayList<>(new LinkedHashSet<>(raw));
        sorted.sort(Comparator.comparingLong(ChapterTransition::tick));
        return sorted;
    }

    private static String chapterAt(List<ChapterTransition> transitions, long tick) {
        String chapter = ChapterType.CHAPTER_1_A_NEW_BEGINNING.name();
        for (ChapterTransition ct : transitions) {
            if (ct.tick() <= tick) chapter = ct.toChapter().name();
        }
        return chapter;
    }

    /** Same line for different quadrants/days counts as one generic beat. */
    private static String genericKey(String text) {
        return text.replaceAll("^Day \\d+\\. ", "").replaceAll("Quadrant \\d", "Quadrant");
    }

    private static List<StoryCaption> distinctCaptions(List<StoryCaption> raw, List<ChapterTransition> transitions) {
        List<StoryCaption> sorted = new ArrayList<>(raw);
        sorted.sort(Comparator.comparingLong(StoryCaption::tick).thenComparingLong(StoryCaption::captionId));
        Set<String> seen = new HashSet<>();
        List<StoryCaption> out = new ArrayList<>();
        for (StoryCaption c : sorted) {
            if (isTransitionEcho(c, transitions)) continue;
            if (seen.add(c.text())) out.add(c);
        }
        return out;
    }

    /** Narrator lines announcing a chapter change are already covered by the transition scene. */
    private static boolean isTransitionEcho(StoryCaption c, List<ChapterTransition> transitions) {
        if (c.text().contains("Hard backstop")) return true;
        for (ChapterTransition ct : transitions) {
            if (ct.tick() == c.tick() && c.text().contains(ct.toChapter().displayName())) return true;
        }
        return false;
    }

    /** Picks up to {@code max} items spread evenly across the (chronological) list. */
    private static List<StoryCaption> thin(List<StoryCaption> items, int max) {
        if (items.size() <= max) return items;
        List<StoryCaption> out = new ArrayList<>(max);
        for (int i = 0; i < max; i++) {
            out.add(items.get((int) ((long) i * items.size() / max)));
        }
        return out;
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
