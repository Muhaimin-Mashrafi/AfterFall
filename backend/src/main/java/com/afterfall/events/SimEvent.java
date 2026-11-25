package com.afterfall.events;

import com.afterfall.domain.EventId;
import com.afterfall.domain.RegionId;
import com.afterfall.domain.SurvivorId;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable canonical simulation event object.
 *
 * <p>All simulation events carry a globally ordered {@link EventId}, tick number,
 * event classification type, affected survivor IDs, optional region location,
 * priority severity, summary, structured data, and causal source references.
 */
public final class SimEvent {

    private final EventId id;
    private final long tick;
    private final String eventType;
    private final List<SurvivorId> targetSurvivors;
    private final Optional<RegionId> location;
    private final EventSeverity severity;
    private final String summary;
    private final Map<String, String> data;
    private final List<EventId> sourceReferences;

    public SimEvent(EventId id,
                    long tick,
                    String eventType,
                    List<SurvivorId> targetSurvivors,
                    Optional<RegionId> location,
                    EventSeverity severity,
                    String summary,
                    Map<String, String> data,
                    List<EventId> sourceReferences) {
        this.id = Objects.requireNonNull(id, "id");
        if (tick < 0) {
            throw new IllegalArgumentException("tick must not be negative");
        }
        this.tick = tick;
        this.eventType = Objects.requireNonNull(eventType, "eventType");
        this.targetSurvivors = List.copyOf(Objects.requireNonNull(targetSurvivors, "targetSurvivors"));
        this.location = Objects.requireNonNull(location, "location");
        this.severity = Objects.requireNonNull(severity, "severity");
        this.summary = Objects.requireNonNull(summary, "summary");
        this.data = Map.copyOf(Objects.requireNonNull(data, "data"));
        this.sourceReferences = List.copyOf(Objects.requireNonNull(sourceReferences, "sourceReferences"));
    }

    public EventId id() {
        return id;
    }

    public long tick() {
        return tick;
    }

    public String eventType() {
        return eventType;
    }

    public List<SurvivorId> targetSurvivors() {
        return targetSurvivors;
    }

    public Optional<RegionId> location() {
        return location;
    }

    public EventSeverity severity() {
        return severity;
    }

    public String summary() {
        return summary;
    }

    public Map<String, String> data() {
        return data;
    }

    public List<EventId> sourceReferences() {
        return sourceReferences;
    }

    public static Builder builder(EventId id, long tick, String eventType) {
        return new Builder(id, tick, eventType);
    }

    public static final class Builder {
        private final EventId id;
        private final long tick;
        private final String eventType;
        private List<SurvivorId> targetSurvivors = List.of();
        private RegionId location;
        private EventSeverity severity = EventSeverity.MINOR;
        private String summary = "";
        private Map<String, String> data = Map.of();
        private List<EventId> sourceReferences = List.of();

        private Builder(EventId id, long tick, String eventType) {
            this.id = Objects.requireNonNull(id, "id");
            this.tick = tick;
            this.eventType = Objects.requireNonNull(eventType, "eventType");
        }

        public Builder targetSurvivors(List<SurvivorId> targetSurvivors) {
            this.targetSurvivors = targetSurvivors;
            return this;
        }

        public Builder targetSurvivor(SurvivorId survivorId) {
            this.targetSurvivors = List.of(survivorId);
            return this;
        }

        public Builder location(RegionId location) {
            this.location = location;
            return this;
        }

        public Builder severity(EventSeverity severity) {
            this.severity = severity;
            return this;
        }

        public Builder summary(String summary) {
            this.summary = summary;
            return this;
        }

        public Builder data(Map<String, String> data) {
            this.data = data;
            return this;
        }

        public Builder sourceReferences(List<EventId> sourceReferences) {
            this.sourceReferences = sourceReferences;
            return this;
        }

        public SimEvent build() {
            return new SimEvent(id, tick, eventType, targetSurvivors, Optional.ofNullable(location),
                    severity, summary, data, sourceReferences);
        }
    }
}
