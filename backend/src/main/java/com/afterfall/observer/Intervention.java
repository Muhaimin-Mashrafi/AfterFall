package com.afterfall.observer;

import com.afterfall.domain.Need;
import com.afterfall.domain.RegionId;
import com.afterfall.domain.SurvivorId;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable command representing an intervention proposed by the ObserverAI.
 *
 * <p>ObserverAI NEVER mutates domain state directly. Instead, it instantiates an
 * {@code Intervention} command and submits it to the simulation command queue.
 * The {@link com.afterfall.simulation.TickCoordinator} validates and applies approved
 * interventions at a safe tick boundary (sole writer).
 */
public final class Intervention {

    private final long id;
    private final long tick;
    private final InterventionType type;
    private final Optional<SurvivorId> targetSurvivor;
    private final Optional<RegionId> targetRegion;
    private final String reason;
    private final String intendedEffect;
    private final String originalReport;
    private final String alteredReport;
    private final Map<Need, Integer> needDeltas;
    private final int trustDelta;
    private final Optional<SurvivorId> relatedSurvivor;

    private Intervention(Builder builder) {
        this.id = builder.id;
        this.tick = builder.tick;
        this.type = Objects.requireNonNull(builder.type, "type");
        this.targetSurvivor = Optional.ofNullable(builder.targetSurvivor);
        this.targetRegion = Optional.ofNullable(builder.targetRegion);
        this.reason = Objects.requireNonNull(builder.reason, "reason");
        this.intendedEffect = Objects.requireNonNull(builder.intendedEffect, "intendedEffect");
        this.originalReport = Objects.requireNonNull(builder.originalReport, "originalReport");
        this.alteredReport = Objects.requireNonNull(builder.alteredReport, "alteredReport");
        this.needDeltas = Collections.unmodifiableMap(new EnumMap<>(builder.needDeltas));
        this.trustDelta = builder.trustDelta;
        this.relatedSurvivor = Optional.ofNullable(builder.relatedSurvivor);
    }

    public long id() {
        return id;
    }

    public long tick() {
        return tick;
    }

    public InterventionType type() {
        return type;
    }

    public Optional<SurvivorId> targetSurvivor() {
        return targetSurvivor;
    }

    public Optional<RegionId> targetRegion() {
        return targetRegion;
    }

    public String reason() {
        return reason;
    }

    public String intendedEffect() {
        return intendedEffect;
    }

    public String originalReport() {
        return originalReport;
    }

    public String alteredReport() {
        return alteredReport;
    }

    public Map<Need, Integer> needDeltas() {
        return needDeltas;
    }

    public int trustDelta() {
        return trustDelta;
    }

    public Optional<SurvivorId> relatedSurvivor() {
        return relatedSurvivor;
    }

    public static Builder builder(long id, long tick, InterventionType type) {
        return new Builder(id, tick, type);
    }

    public static final class Builder {
        private final long id;
        private final long tick;
        private final InterventionType type;
        private SurvivorId targetSurvivor;
        private RegionId targetRegion;
        private String reason = "";
        private String intendedEffect = "";
        private String originalReport = "";
        private String alteredReport = "";
        private final Map<Need, Integer> needDeltas = new EnumMap<>(Need.class);
        private int trustDelta = 0;
        private SurvivorId relatedSurvivor;

        private Builder(long id, long tick, InterventionType type) {
            this.id = id;
            this.tick = tick;
            this.type = Objects.requireNonNull(type, "type");
        }

        public Builder targetSurvivor(SurvivorId targetSurvivor) {
            this.targetSurvivor = targetSurvivor;
            return this;
        }

        public Builder targetRegion(RegionId targetRegion) {
            this.targetRegion = targetRegion;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public Builder intendedEffect(String intendedEffect) {
            this.intendedEffect = intendedEffect;
            return this;
        }

        public Builder originalReport(String originalReport) {
            this.originalReport = originalReport;
            return this;
        }

        public Builder alteredReport(String alteredReport) {
            this.alteredReport = alteredReport;
            return this;
        }

        public Builder needDelta(Need need, int delta) {
            this.needDeltas.put(Objects.requireNonNull(need, "need"), delta);
            return this;
        }

        public Builder trustDelta(int trustDelta) {
            this.trustDelta = trustDelta;
            return this;
        }

        public Builder relatedSurvivor(SurvivorId relatedSurvivor) {
            this.relatedSurvivor = relatedSurvivor;
            return this;
        }

        public Intervention build() {
            return new Intervention(this);
        }
    }
}
