package com.afterfall.application;

import com.afterfall.chapter.ChapterTransition;
import com.afterfall.infrastructure.persistence.inmemory.InMemoryEventRecorder;
import com.afterfall.infrastructure.persistence.inmemory.InMemoryEvidenceDiscoveryRepository;
import com.afterfall.infrastructure.persistence.inmemory.InMemoryRunRepository;
import com.afterfall.web.dto.StateResponse;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/** Pacing guard: the default demo run must show organic chapters, a famine and deaths, deterministically. */
class DefaultRunPacingTest {

    private record Outcome(List<String> transitions, boolean famineSeen, int dead, int alive) { }

    private Outcome run(int ticks) throws Exception {
        InMemoryRunRepository repo = new InMemoryRunRepository();
        SimulationService service = new SimulationService(repo, new InMemoryEventRecorder(), new InMemoryEvidenceDiscoveryRepository());
        boolean famineSeen = false;
        StateResponse state = null;
        for (int i = 0; i < ticks; i++) {
            state = service.stepTick();
            famineSeen |= state.resources().get("food") <= 10;
        }
        assertThat(service.eventBus().drainAll(5, TimeUnit.SECONDS)).isTrue();
        List<String> transitions = new ArrayList<>();
        for (ChapterTransition t : repo.chapterTransitionsForRun(com.afterfall.application.persistence.RunId.of("run-1"))) {
            assertThat(t.isForced()).as("transition at tick %d must be threshold-driven", t.tick()).isFalse();
            transitions.add(t.tick() + ":" + t.toChapter());
        }
        return new Outcome(transitions, famineSeen, state.deceasedSurvivors(), state.aliveSurvivors());
    }

    @Test
    void defaultRunReachesAllChaptersOrganicallyWithFamineAndDeaths() throws Exception {
        Outcome o = run(150);
        assertThat(o.transitions()).hasSize(3);
        assertThat(o.famineSeen()).isTrue();
        assertThat(o.dead()).isPositive();
    }

    @Test
    void defaultRunIsDeterministic() throws Exception {
        assertThat(run(150)).isEqualTo(run(150));
    }
}
