package com.afterfall.events;

import com.afterfall.domain.EventId;
import com.afterfall.domain.RegionId;
import com.afterfall.domain.SurvivorId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SimEventTest {

    @Test
    void eventIsImmutableAndPreservesAttributes() {
        SimEvent event = new SimEvent(
                new EventId(1L),
                5L,
                "SURVIVOR_WORK",
                List.of(new SurvivorId(2)),
                Optional.of(RegionId.REGION_1),
                EventSeverity.MINOR,
                "Work summary",
                Map.of("hours", "8"),
                List.of(new EventId(99L))
        );

        assertThat(event.id().value()).isEqualTo(1L);
        assertThat(event.tick()).isEqualTo(5L);
        assertThat(event.eventType()).isEqualTo("SURVIVOR_WORK");
        assertThat(event.targetSurvivors()).containsExactly(new SurvivorId(2));
        assertThat(event.location()).contains(RegionId.REGION_1);
        assertThat(event.severity()).isEqualTo(EventSeverity.MINOR);
        assertThat(event.summary()).isEqualTo("Work summary");
        assertThat(event.data()).containsEntry("hours", "8");
        assertThat(event.sourceReferences()).containsExactly(new EventId(99L));
    }

    @Test
    void rejectsNegativeTick() {
        assertThatThrownBy(() -> new SimEvent(
                new EventId(1L), -1L, "TEST", List.of(), Optional.empty(), EventSeverity.MINOR, "S", Map.of(), List.of()
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void builderConstructsValidEvent() {
        SimEvent event = SimEvent.builder(new EventId(10L), 2L, "HEAL")
                .targetSurvivor(new SurvivorId(3))
                .location(RegionId.REGION_2)
                .severity(EventSeverity.SIGNIFICANT)
                .summary("Recovered from illness")
                .build();

        assertThat(event.id().value()).isEqualTo(10L);
        assertThat(event.severity()).isEqualTo(EventSeverity.SIGNIFICANT);
        assertThat(event.targetSurvivors()).containsExactly(new SurvivorId(3));
    }
}
