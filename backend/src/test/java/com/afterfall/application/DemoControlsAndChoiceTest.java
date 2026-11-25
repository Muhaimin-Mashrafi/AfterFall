package com.afterfall.application;

import com.afterfall.infrastructure.persistence.inmemory.InMemoryEventRecorder;
import com.afterfall.infrastructure.persistence.inmemory.InMemoryEvidenceDiscoveryRepository;
import com.afterfall.infrastructure.persistence.inmemory.InMemoryRunRepository;
import com.afterfall.recap.RecordingScene;
import com.afterfall.web.dto.StateResponse;
import com.afterfall.web.dto.StoryResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Regression: the choice dialog data is exposed, START auto-advances, the recap is varied. */
class DemoControlsAndChoiceTest {

    private SimulationService service(long intervalMs) {
        SimulationService s = new SimulationService(new InMemoryRunRepository(), new InMemoryEventRecorder(),
                new InMemoryEvidenceDiscoveryRepository());
        s.setAutoRunIntervalMs(intervalMs);
        return s;
    }

    private void awaitTrue(java.util.function.BooleanSupplier c) throws Exception {
        long end = System.currentTimeMillis() + 60_000;
        while (!c.getAsBoolean()) {
            if (System.currentTimeMillis() > end) throw new AssertionError("timed out");
            Thread.sleep(10);
        }
    }

    @Test
    void pausedChoiceIsExposedAnsweredAndResumes() {
        SimulationService s = service(0);
        StateResponse st = s.stepTick();
        while (!st.hasPendingChoice()) st = s.stepTick();
        StoryResponse.ChoiceMomentDto c = s.getStory().activeChoice();
        assertThat(c).isNotNull();
        assertThat(c.choiceId()).isNotBlank();
        assertThat(c.title()).isNotBlank();
        assertThat(c.options()).hasSizeGreaterThanOrEqualTo(2);

        StateResponse after = s.choose(c.choiceId(), c.options().get(1).optionId());
        assertThat(after.hasPendingChoice()).isFalse();
        assertThat(s.getStory().activeChoice()).isNull();
        long tick = after.tick();
        assertThat(s.stepTick().tick()).isEqualTo(tick + 1);
    }

    @Test
    void startAdvancesTimeOnItsOwnStopsAtChoiceAndResumesAfterAnswer() throws Exception {
        SimulationService s = service(5);
        try {
            s.startSimulation();
            awaitTrue(() -> s.getCurrentState().tick() >= 5);
            s.pauseSimulation();
            long paused = s.getCurrentState().tick();
            Thread.sleep(80);
            assertThat(s.getCurrentState().tick()).isEqualTo(paused);

            s.resumeSimulation();
            awaitTrue(() -> s.getCurrentState().hasPendingChoice());
            assertThat(s.getCurrentState().lifecycle()).isEqualTo("PAUSED");
            long atChoice = s.getCurrentState().tick();
            Thread.sleep(80);
            assertThat(s.getCurrentState().tick()).isEqualTo(atChoice);

            StoryResponse.ChoiceMomentDto c = s.getStory().activeChoice();
            s.choose(c.choiceId(), c.options().get(0).optionId());
            awaitTrue(() -> s.getCurrentState().tick() > atChoice + 3);
        } finally {
            s.shutdownAutoRunner();
        }
    }

    @Test
    void recapIsVariedAndNamesSurvivors() {
        SimulationService s = service(0);
        for (int i = 0; i < 230; i++) {
            StateResponse st = s.stepTick();
            if (st.hasPendingChoice()) {
                StoryResponse.ChoiceMomentDto c = s.getStory().activeChoice();
                s.choose(c.choiceId(), c.options().get(0).optionId());
            }
        }
        List<RecordingScene> scenes = s.getRecap().finalRecording().scenes();
        long observer = scenes.stream().filter(x -> x.narrativeText().contains("Observer record")).count();
        assertThat(observer).isBetween(1L, 3L);
        assertThat(scenes).extracting(RecordingScene::narrativeText).doesNotHaveDuplicates();
        assertThat(scenes.stream().filter(x -> !x.referencedSurvivorIds().isEmpty()).count()).isGreaterThanOrEqualTo(3);
        assertThat(scenes.stream().anyMatch(x -> x.narrativeText().contains("journey had ended")
                || x.narrativeText().contains("did not answer"))).isTrue();
        assertThat(s.getRecap().finalRecording().closingLine())
                .isEqualTo("The settlement fell silent. The Observer remains active.");
    }
}
