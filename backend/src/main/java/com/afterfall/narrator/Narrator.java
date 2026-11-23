package com.afterfall.narrator;

import com.afterfall.domain.EventId;
import com.afterfall.domain.Survivor;
import com.afterfall.domain.SurvivorId;
import com.afterfall.domain.Settlement;
import com.afterfall.events.EventSeverity;
import com.afterfall.events.EventSubscriber;
import com.afterfall.events.SimEvent;
import com.afterfall.simulation.SeededRandomFactory;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Narrator: Independent event consumer and storytelling engine.
 *
 * <p>Requirements:
 * <ul>
 *   <li>Subscribes to {@link com.afterfall.events.EventBus} as an independent consumer.</li>
 *   <li>Evaluates event priorities (MINOR, SIGNIFICANT, CRITICAL).</li>
 *   <li>Selects authored templates deterministically using seeded pseudo-random logic.</li>
 *   <li>Never invents simulation facts; links all {@link StoryCaption}s to source {@link EventId}s.</li>
 *   <li>Maintains a bounded anti-repetition buffer to prevent monotone consecutive phrasing.</li>
 *   <li>Adheres strictly to implied, non-graphic cinematic register.</li>
 * </ul>
 */
public final class Narrator implements EventSubscriber {

    public static final String SUBSCRIBER_ID = "narrator";
    private static final int ANTI_REPETITION_BUFFER_SIZE = 8;

    private final Settlement settlement;
    private final SeededRandomFactory randomFactory;
    private final List<StoryCaption> generatedCaptions = new CopyOnWriteArrayList<>();
    private final Deque<String> recentTemplateIds = new ArrayDeque<>();
    private long nextCaptionId = 1L;

    public Narrator(Settlement settlement, SeededRandomFactory randomFactory) {
        this.settlement = Objects.requireNonNull(settlement, "settlement");
        this.randomFactory = Objects.requireNonNull(randomFactory, "randomFactory");
    }

    @Override
    public String subscriberId() {
        return SUBSCRIBER_ID;
    }

    @Override
    public synchronized void onEvents(List<SimEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }

        boolean narratedAny = false;
        for (SimEvent event : events) {
            // Prioritise CRITICAL and SIGNIFICANT
            if (event.severity() == EventSeverity.CRITICAL || event.severity() == EventSeverity.SIGNIFICANT) {
                generateCaption(event);
                narratedAny = true;
            } else if (event.id().value() % 2 == 0) {
                // Sample MINOR events deterministically (every 2nd event)
                generateCaption(event);
                narratedAny = true;
            }
        }

        // If a batch had only MINOR events and none were sampled, narrate at least the first event to preserve scene continuity
        if (!narratedAny && !events.isEmpty()) {
            generateCaption(events.get(0));
        }
    }

    private void generateCaption(SimEvent event) {
        List<NarrativeTemplate> candidates = NarrativeTemplateRepository.templatesFor(event.eventType());
        if (candidates.isEmpty()) {
            // Fallback generic template
            candidates = List.of(new NarrativeTemplate("GENERIC", event.eventType(), "Day {day}. In {region}, {survivor} experienced: " + event.summary()));
        }

        // Filter out recently used templates if alternatives exist
        List<NarrativeTemplate> available = candidates.stream()
                .filter(t -> !recentTemplateIds.contains(t.id()))
                .toList();

        if (available.isEmpty()) {
            available = candidates;
        }

        // Deterministic template selection
        int seedHash = (int) (randomFactory.runSeed() ^ (event.id().value() << 16) ^ (event.tick() * 31));
        int index = Math.abs(seedHash) % available.size();
        NarrativeTemplate selected = available.get(index);

        // Update anti-repetition buffer
        recentTemplateIds.addLast(selected.id());
        if (recentTemplateIds.size() > ANTI_REPETITION_BUFFER_SIZE) {
            recentTemplateIds.removeFirst();
        }

        // Render template with verified domain entities
        String survivorName = "Someone";
        if (!event.targetSurvivors().isEmpty()) {
            SurvivorId sId = event.targetSurvivors().get(0);
            survivorName = settlement.survivor(sId).map(Survivor::name).orElse("Survivor #" + sId.value());
        }

        String targetName = "";
        if (event.targetSurvivors().size() > 1) {
            SurvivorId tId = event.targetSurvivors().get(1);
            targetName = settlement.survivor(tId).map(Survivor::name).orElse("Survivor #" + tId.value());
        }

        String regionName = event.location().map(r -> "Quadrant " + r.value()).orElse("the settlement");
        long day = (event.tick() / 4) + 1; // 4 ticks per day in default model

        String renderedText = selected.render(survivorName, targetName, regionName, day, event.summary());

        StoryCaption caption = new StoryCaption(
                nextCaptionId++,
                event.tick(),
                renderedText,
                event.severity(),
                List.of(event.id()),
                event.targetSurvivors()
        );

        generatedCaptions.add(caption);
    }

    public List<StoryCaption> captions() {
        return Collections.unmodifiableList(generatedCaptions);
    }

    public void clearCaptions() {
        generatedCaptions.clear();
        recentTemplateIds.clear();
    }
}
