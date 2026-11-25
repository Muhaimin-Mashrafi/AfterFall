package com.afterfall.application;

import com.afterfall.application.persistence.RunId;
import com.afterfall.chapter.ChapterTransition;
import com.afterfall.infrastructure.persistence.inmemory.InMemoryEventRecorder;
import com.afterfall.infrastructure.persistence.inmemory.InMemoryEvidenceDiscoveryRepository;
import com.afterfall.infrastructure.persistence.inmemory.InMemoryRunRepository;
import com.afterfall.recap.RecordingScene;
import com.afterfall.recap.RunRecap;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/** Regression: the recap of a real service run reflects persisted story beats, not repeated transitions. */
class RecapFromRealRunTest {

    @Test
    void recapOfRealRunHasDistinctBeatsAndTrueEndingChapter() throws Exception {
        InMemoryRunRepository runRepo = new InMemoryRunRepository();
        SimulationService service = new SimulationService(runRepo, new InMemoryEventRecorder(), new InMemoryEvidenceDiscoveryRepository());
        for (int i = 0; i < 400; i++) {
            service.stepTick();
        }
        assertThat(service.eventBus().drainAll(5, TimeUnit.SECONDS)).isTrue();

        // Each transition is persisted exactly once, however many ticks ran afterwards.
        List<ChapterTransition> persisted = runRepo.chapterTransitionsForRun(RunId.of("run-1"));
        assertThat(persisted).hasSize(new HashSet<>(persisted).size());

        RunRecap recap = service.getRecap();
        assertThat(recap.chapterArc()).hasSameSizeAs(persisted);
        assertThat(recap.endingChapter()).isEqualTo(persisted.get(persisted.size() - 1).toChapter().name());
        assertThat(recap.finalRecording().endingChapter()).isEqualTo(recap.endingChapter());

        List<RecordingScene> scenes = recap.finalRecording().scenes();
        assertThat(scenes).hasSizeGreaterThan(3);
        assertThat(scenes.stream().map(RecordingScene::narrativeText).distinct().count()).isEqualTo(scenes.size());
        assertThat(scenes).noneMatch(s -> s.narrativeText().contains("Hard backstop"));
        assertThat(recap.finalRecording().closingLine()).isEqualTo("The settlement fell silent. The Observer remains active.");
    }
}
