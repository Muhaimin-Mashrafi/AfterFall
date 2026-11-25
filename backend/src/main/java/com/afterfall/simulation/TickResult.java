package com.afterfall.simulation;

import com.afterfall.action.ProposedEvent;
import com.afterfall.action.ResourceClaim;

import java.util.List;
import java.util.Objects;

/**
 * Immutable result of one completed simulation tick.
 * Contains ordered region results, collected events, resource claims,
 * migration requests, and summary counts.
 */
public final class TickResult {

    private final long tickNumber;
    private final long worldVersion;
    private final List<RegionResult> regionResults;
    private final List<ProposedEvent> events;
    private final List<ResourceClaim> resourceClaims;
    private final List<MigrationRequest> migrationRequests;
    private final int survivorsProcessed;
    private final int actionsAccepted;
    private final int actionsRejected;
    private final boolean success;
    private final String failureReason;

    private TickResult(Builder b) {
        this.tickNumber = b.tickNumber;
        this.worldVersion = b.worldVersion;
        this.regionResults = List.copyOf(b.regionResults);
        this.events = List.copyOf(b.events);
        this.resourceClaims = List.copyOf(b.resourceClaims);
        this.migrationRequests = List.copyOf(b.migrationRequests);
        this.survivorsProcessed = b.survivorsProcessed;
        this.actionsAccepted = b.actionsAccepted;
        this.actionsRejected = b.actionsRejected;
        this.success = b.success;
        this.failureReason = b.failureReason;
    }

    public long tickNumber() { return tickNumber; }
    public long worldVersion() { return worldVersion; }
    public List<RegionResult> regionResults() { return regionResults; }
    public List<ProposedEvent> events() { return events; }
    public List<ResourceClaim> resourceClaims() { return resourceClaims; }
    public List<MigrationRequest> migrationRequests() { return migrationRequests; }
    public int survivorsProcessed() { return survivorsProcessed; }
    public int actionsAccepted() { return actionsAccepted; }
    public int actionsRejected() { return actionsRejected; }
    public boolean isSuccess() { return success; }
    public String failureReason() { return failureReason; }

    public static Builder builder(long tickNumber, long worldVersion) {
        return new Builder(tickNumber, worldVersion);
    }

    public static final class Builder {
        private final long tickNumber;
        private final long worldVersion;
        private List<RegionResult> regionResults = List.of();
        private List<ProposedEvent> events = List.of();
        private List<ResourceClaim> resourceClaims = List.of();
        private List<MigrationRequest> migrationRequests = List.of();
        private int survivorsProcessed;
        private int actionsAccepted;
        private int actionsRejected;
        private boolean success = true;
        private String failureReason;

        private Builder(long tickNumber, long worldVersion) {
            this.tickNumber = tickNumber;
            this.worldVersion = worldVersion;
        }

        public Builder regionResults(List<RegionResult> r) { this.regionResults = Objects.requireNonNull(r); return this; }
        public Builder events(List<ProposedEvent> e) { this.events = Objects.requireNonNull(e); return this; }
        public Builder resourceClaims(List<ResourceClaim> rc) { this.resourceClaims = Objects.requireNonNull(rc); return this; }
        public Builder migrationRequests(List<MigrationRequest> mr) { this.migrationRequests = Objects.requireNonNull(mr); return this; }
        public Builder survivorsProcessed(int n) { this.survivorsProcessed = n; return this; }
        public Builder actionsAccepted(int n) { this.actionsAccepted = n; return this; }
        public Builder actionsRejected(int n) { this.actionsRejected = n; return this; }
        public Builder failed(String reason) { this.success = false; this.failureReason = reason; return this; }

        public TickResult build() { return new TickResult(this); }
    }
}
