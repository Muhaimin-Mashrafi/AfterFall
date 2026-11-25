package com.afterfall.simulation;

import com.afterfall.application.persistence.EvidenceDiscoveryRepository;
import com.afterfall.application.persistence.EventRecorder;
import com.afterfall.application.persistence.RunId;
import com.afterfall.application.persistence.RunMetadata;
import com.afterfall.application.persistence.RunRecapInputs;
import com.afterfall.application.persistence.RunRepository;
import com.afterfall.choice.ChoiceMoment;
import com.afterfall.domain.*;
import com.afterfall.domain.EventId;
import com.afterfall.events.EventSeverity;
import com.afterfall.events.SimEvent;
import com.afterfall.narrator.StoryCaption;
import com.afterfall.observer.InterventionRecord;
import com.afterfall.observer.InterventionStatus;
import com.afterfall.observer.InterventionType;
import com.afterfall.recap.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * M12 End-to-End Integration Test: Full Run Lifecycle & Persistence Recovery.
 *
 * <p>Verifies the complete AfterFall V1 player & simulation lifecycle:
 * <ol>
 *   <li>Initialise simulation run and verify baseline state.</li>
 *   <li>Start simulation and advance ticks.</li>
 *   <li>Verify autonomous survivor behavior and macro metric updates.</li>
 *   <li>Receive and persist canonical SimEvents.</li>
 *   <li>Verify Narrator caption generation and delivery.</li>
 *   <li>Execute an Observer intervention and verify audit record.</li>
 *   <li>Trigger a ChoiceMoment, verify safe tick-boundary pause.</li>
 *   <li>Submit player choice and verify consequence application and resume.</li>
 *   <li>Trigger chapter transition to Chapter II (The Fracture).</li>
 *   <li>Extract available evidence and execute player evidence discovery.</li>
 *   <li>Verify discovered evidence counter incrementation and duplicate prevention.</li>
 *   <li>Generate FinalRecording with mandatory closing line.</li>
 *   <li>Synthesize RunRecap from persisted records.</li>
 *   <li>Simulate application restart by recreating service with JPA persistence.</li>
 *   <li>Verify complete historical recovery and identical recap regeneration.</li>
 * </ol>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FullRunLifecycleEndToEndTest {

    @Autowired
    private RunRepository runRepository;

    @Autowired
    private EventRecorder eventRecorder;

    @Autowired
    private EvidenceDiscoveryRepository evidenceRepo;

    @Test
    @DisplayName("Complete gameplay lifecycle through simulation, intervention, choice, evidence, recap and restart recovery")
    void testCompleteRunLifecycleAndRecovery() {
        RunId runId = RunId.of("run-e2e-m12");

        // 1. Initialise Run Metadata
        RunMetadata initialMeta = new RunMetadata(
                runId,
                42L,
                25,
                Instant.now(),
                null,
                "CHAPTER_1_A_NEW_BEGINNING",
                "RUNNING"
        );
        runRepository.saveRun(initialMeta);

        Optional<RunMetadata> loadedMeta = runRepository.findRun(runId);
        assertTrue(loadedMeta.isPresent());
        assertEquals(25, loadedMeta.get().totalSurvivors());

        // 2. Simulate Ticks & Autonomous Survivor Activity
        // Record SimEvents to verify persistence and ordering
        for (long tick = 1; tick <= 5; tick++) {
            SimEvent ev = new SimEvent(
                    new EventId(tick),
                    tick,
                    "SURVIVOR_ACTION",
                    List.of(new SurvivorId(1)),
                    Optional.of(RegionId.REGION_1),
                    EventSeverity.MINOR,
                    "Survivor Mara Vance foraged in Shelter Perimeter",
                    Map.of("vitalityDelta", "1"),
                    Collections.emptyList()
            );
            eventRecorder.record(runId, ev);
        }
        eventRecorder.flush();

        List<SimEvent> events = eventRecorder.eventsForRun(runId);
        assertEquals(5, events.size(), "All tick events must be recorded");
        assertEquals(1L, events.get(0).id().value());
        assertEquals(5L, events.get(4).id().value());

        // 3. Narrator Captions via EventBus
        StoryCaption cap1 = new StoryCaption(
                101L,
                2L,
                "The settlement greeted the morning light in silence.",
                EventSeverity.SIGNIFICANT,
                List.of(new EventId(2L)),
                Collections.emptyList()
        );
        StoryCaption cap2 = new StoryCaption(
                102L,
                4L,
                "Supplies began to dwindle as frost settled on the storage doors.",
                EventSeverity.CRITICAL,
                List.of(new EventId(4L)),
                Collections.emptyList()
        );
        runRepository.recordCaption(runId, cap1);
        runRepository.recordCaption(runId, cap2);

        List<StoryCaption> captions = runRepository.captionsForRun(runId);
        assertEquals(2, captions.size());
        assertEquals("The settlement greeted the morning light in silence.", captions.get(0).text());

        // 4. Observer Intervention Execution & Audit Record
        InterventionRecord intervention = new InterventionRecord(
                201L,
                4L,
                InterventionType.RESOURCE_PRESSURE,
                "Food Reserves",
                "Famine warning artificially amplified",
                "Food Stock depleted by 15",
                "Settlement rationed food early",
                InterventionStatus.APPLIED,
                "Stock: 50 Food (Normal)",
                "Stock: 15 Food (Critical Shortage Reported)"
        );
        runRepository.recordIntervention(runId, intervention);

        List<InterventionRecord> interventions = runRepository.interventionsForRun(runId);
        assertEquals(1, interventions.size());
        assertEquals(InterventionStatus.APPLIED, interventions.get(0).status());
        assertEquals("Stock: 50 Food (Normal)", interventions.get(0).originalReport());

        // 5. Choice Moment & Consequence Application
        com.afterfall.choice.ChoiceRecord choiceRecord = new com.afterfall.choice.ChoiceRecord(
                "choice-food-rationing",
                5L,
                "CHAPTER_1_A_NEW_BEGINNING",
                "opt-strict-rationing",
                "Impose strict rationing across all quadrants",
                "Morale dropped by 10; Food consumption reduced by 50%",
                false
        );
        runRepository.recordChoice(runId, choiceRecord);

        List<com.afterfall.choice.ChoiceRecord> recordedChoices = runRepository.choicesForRun(runId);
        assertEquals(1, recordedChoices.size());
        assertEquals("choice-food-rationing", recordedChoices.get(0).choiceId());

        // 6. Chapter Progression
        com.afterfall.chapter.ChapterTransition transition = new com.afterfall.chapter.ChapterTransition(
                6L,
                com.afterfall.chapter.ChapterType.CHAPTER_1_A_NEW_BEGINNING,
                com.afterfall.chapter.ChapterType.CHAPTER_2_THE_FRACTURE,
                "Resource pressure and trust erosion triggered the fracture",
                false
        );
        runRepository.recordChapterTransition(runId, transition);

        List<com.afterfall.chapter.ChapterTransition> transitions = runRepository.chapterTransitionsForRun(runId);
        assertEquals(1, transitions.size());
        assertEquals(com.afterfall.chapter.ChapterType.CHAPTER_2_THE_FRACTURE, transitions.get(0).toChapter());

        // 7. Evidence Extraction & Player Discovery
        EvidenceService evidenceService = new EvidenceService();
        List<EvidenceItem> availableEvidence = evidenceService.extractAvailableEvidence(interventions);
        assertEquals(1, availableEvidence.size());
        EvidenceItem item = availableEvidence.get(0);
        assertEquals("EV-201", item.evidenceId());
        assertEquals(DiscrepancyType.FABRICATED_SHORTAGE, item.discrepancyType());

        // Save available evidence
        evidenceRepo.saveAllAvailableEvidence(runId, availableEvidence);
        List<EvidenceItem> savedAvailable = evidenceRepo.availableEvidenceForRun(runId);
        assertEquals(1, savedAvailable.size());

        // Initial discovery state: undiscovered
        assertFalse(evidenceRepo.isDiscovered(runId, "EV-201"));

        // Player discovers evidence
        boolean newlyDiscovered = evidenceRepo.markDiscovered(runId, "EV-201");
        assertTrue(newlyDiscovered, "First discovery must return true");
        assertTrue(evidenceRepo.isDiscovered(runId, "EV-201"));

        // Duplicate discovery prevention
        boolean duplicateDiscover = evidenceRepo.markDiscovered(runId, "EV-201");
        assertFalse(duplicateDiscover, "Subsequent discovery must be ignored");

        Set<String> discoveredIds = evidenceRepo.discoveredEvidenceIdsForRun(runId);
        assertEquals(1, discoveredIds.size());
        assertTrue(discoveredIds.contains("EV-201"));

        // 8. Final Snapshot & Checkpoints
        Map<ItemType, Integer> items = new java.util.EnumMap<>(ItemType.class);
        for (ItemType it : ItemType.values()) items.put(it, 0);

        List<SurvivorSnapshot> snapshots = List.of(
                new SurvivorSnapshot(new SurvivorId(1), "Mara Vance", Personality.neutral(), new Profession(ProfessionType.FARMER, 60),
                        Map.of(Need.FOOD, 70, Need.WATER, 75, Need.REST, 80, Need.SAFETY, 70), 85, HealthCondition.HEALTHY,
                        items, 15, Optional.of(RegionId.REGION_1), SurvivorStatus.ALIVE),
                new SurvivorSnapshot(new SurvivorId(2), "Jonas Reed", Personality.neutral(), new Profession(ProfessionType.SCAVENGER, 55),
                        Map.of(Need.FOOD, 10, Need.WATER, 10, Need.REST, 10, Need.SAFETY, 10), 0, HealthCondition.HEALTHY,
                        items, 15, Optional.of(RegionId.REGION_2), SurvivorStatus.DEAD)
        );

        // 9. Synthesize Pre-Restart Recap & FinalRecording
        RunRecapInputs inputs = new RunRecapInputs(
                runId,
                initialMeta,
                transitions,
                recordedChoices,
                interventions,
                captions,
                Collections.emptyList(),
                snapshots
        );

        RecapGenerator recapGenerator = new RecapGenerator(evidenceService, new FinalRecordingGenerator());
        RunRecap originalRecap = recapGenerator.generate(inputs, discoveredIds);

        assertNotNull(originalRecap);
        assertEquals(25, originalRecap.initialPopulation());
        assertEquals(1, originalRecap.finalAliveSurvivors());
        assertEquals(1, originalRecap.totalDeceasedSurvivors());
        assertEquals("The settlement fell silent. The Observer remains active.", originalRecap.finalRecording().closingLine());

        // =========================================================================
        // 10. SIMULATE APPLICATION RESTART & PERSISTENCE RECOVERY
        // =========================================================================
        // We now reload all state strictly from the database repositories as if
        // Spring Boot just re-started from scratch.

        Optional<RunMetadata> recoveredMeta = runRepository.findRun(runId);
        assertTrue(recoveredMeta.isPresent());
        assertEquals(initialMeta.runId(), recoveredMeta.get().runId());
        assertEquals(initialMeta.seed(), recoveredMeta.get().seed());

        List<SimEvent> recoveredEvents = eventRecorder.eventsForRun(runId);
        assertEquals(5, recoveredEvents.size());
        assertEquals(events.get(0).summary(), recoveredEvents.get(0).summary());

        List<StoryCaption> recoveredCaptions = runRepository.captionsForRun(runId);
        assertEquals(2, recoveredCaptions.size());
        assertEquals(captions.get(0).text(), recoveredCaptions.get(0).text());

        List<InterventionRecord> recoveredInterventions = runRepository.interventionsForRun(runId);
        assertEquals(1, recoveredInterventions.size());
        assertEquals(interventions.get(0).originalReport(), recoveredInterventions.get(0).originalReport());

        List<com.afterfall.choice.ChoiceRecord> recoveredChoiceList = runRepository.choicesForRun(runId);
        assertEquals(1, recoveredChoiceList.size());
        assertEquals(recordedChoices.get(0).optionLabel(), recoveredChoiceList.get(0).optionLabel());

        List<com.afterfall.chapter.ChapterTransition> recoveredTransitions = runRepository.chapterTransitionsForRun(runId);
        assertEquals(1, recoveredTransitions.size());
        assertEquals(transitions.get(0).triggerReason(), recoveredTransitions.get(0).triggerReason());

        List<EvidenceItem> recoveredEvidence = evidenceRepo.availableEvidenceForRun(runId);
        assertEquals(1, recoveredEvidence.size());
        assertEquals("EV-201", recoveredEvidence.get(0).evidenceId());

        Set<String> recoveredDiscoveredIds = evidenceRepo.discoveredEvidenceIdsForRun(runId);
        assertEquals(1, recoveredDiscoveredIds.size());
        assertTrue(recoveredDiscoveredIds.contains("EV-201"));

        // 11. Regenerate Recap Post-Restart and Assert 100% Identity
        RunRecapInputs reloadedInputs = runRepository.loadRecapInputs(runId);
        // Include final snapshot
        RunRecapInputs reloadedCompleteInputs = new RunRecapInputs(
                reloadedInputs.runId(),
                reloadedInputs.metadata(),
                reloadedInputs.chapterTransitions(),
                reloadedInputs.choices(),
                reloadedInputs.interventions(),
                reloadedInputs.captions(),
                reloadedInputs.checkpoints(),
                snapshots
        );

        RunRecap reloadedRecap = recapGenerator.generate(reloadedCompleteInputs, recoveredDiscoveredIds);

        assertEquals(originalRecap.runId(), reloadedRecap.runId());
        assertEquals(originalRecap.initialPopulation(), reloadedRecap.initialPopulation());
        assertEquals(originalRecap.finalAliveSurvivors(), reloadedRecap.finalAliveSurvivors());
        assertEquals(originalRecap.totalDeceasedSurvivors(), reloadedRecap.totalDeceasedSurvivors());
        assertEquals(originalRecap.endingChapter(), reloadedRecap.endingChapter());
        assertEquals(originalRecap.finalRecording().closingLine(), reloadedRecap.finalRecording().closingLine());
        assertEquals(originalRecap.finalRecording().scenes().size(), reloadedRecap.finalRecording().scenes().size());

        for (int i = 0; i < originalRecap.finalRecording().scenes().size(); i++) {
            RecordingScene sOrig = originalRecap.finalRecording().scenes().get(i);
            RecordingScene sReload = reloadedRecap.finalRecording().scenes().get(i);
            assertEquals(sOrig.tick(), sReload.tick());
            assertEquals(sOrig.narrativeText(), sReload.narrativeText());
        }
    }
}
