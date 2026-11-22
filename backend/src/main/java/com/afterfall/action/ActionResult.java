package com.afterfall.action;

import com.afterfall.domain.HealthCondition;
import com.afterfall.domain.ItemType;
import com.afterfall.domain.Need;
import com.afterfall.domain.RegionId;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;


/**
 * Immutable evaluation result produced by evaluating an {@link Action}.
 * Contains typed proposals for deltas and events without mutating domain models.
 */
public record ActionResult(
        ActionType actionType,
        ActionStatus status,
        Optional<String> rejectionReason,
        Map<Need, Integer> needDeltas,
        int vitalityDelta,
        Optional<HealthCondition> newCondition,
        Map<ItemType, Integer> inventoryDeltas,
        int workContribution,
        Optional<RegionId> migrationRequest,
        List<ProposedEvent> eventProposals,
        List<ResourceClaim> resourceClaims


) {

    public ActionResult {
        Objects.requireNonNull(actionType, "actionType");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(rejectionReason, "rejectionReason");
        Objects.requireNonNull(needDeltas, "needDeltas");
        Objects.requireNonNull(newCondition, "newCondition");
        Objects.requireNonNull(inventoryDeltas, "inventoryDeltas");
        Objects.requireNonNull(migrationRequest, "migrationRequest");
        Objects.requireNonNull(eventProposals, "eventProposals");
        Objects.requireNonNull(resourceClaims, "resourceClaims");

        // Invariants based on status
        if (status == ActionStatus.REJECTED) {
            if (rejectionReason.isEmpty()) {
                throw new IllegalArgumentException("Rejected ActionResult must have a rejection reason");
            }
            if (!needDeltas.isEmpty() || !inventoryDeltas.isEmpty() || vitalityDelta != 0 || newCondition.isPresent()
                    || workContribution != 0 || migrationRequest.isPresent() || !eventProposals.isEmpty() || !resourceClaims.isEmpty()) {
                throw new IllegalArgumentException("Rejected ActionResult must not contain any proposals or claims");
            }
            // Ensure empty immutable collections
            needDeltas = Collections.emptyMap();
            inventoryDeltas = Collections.emptyMap();
            eventProposals = List.of();
            resourceClaims = List.of();
        } else { // SUCCESS
            if (rejectionReason.isPresent()) {
                throw new IllegalArgumentException("Successful ActionResult must not have a rejection reason");
            }
            if (workContribution < 0) {
                throw new IllegalArgumentException("workContribution cannot be negative");
            }
            needDeltas = needDeltas.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(new EnumMap<>(needDeltas));
            inventoryDeltas = inventoryDeltas.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(new EnumMap<>(inventoryDeltas));
            eventProposals = List.copyOf(eventProposals);
            resourceClaims = List.copyOf(resourceClaims);
        }
    }

    public static ActionResult rejected(ActionType type, String reason) {
        Objects.requireNonNull(reason, "reason");
        return new ActionResult(
                type,
                ActionStatus.REJECTED,
                Optional.of(reason),
                Collections.emptyMap(),
                0,
                Optional.empty(),
                Collections.emptyMap(),
                0,
                Optional.empty(),
                Collections.emptyList(),
                List.of()
        );
    }

    public static Builder builder(ActionType type) {
        return new Builder(type);
    }

    public boolean isSuccess() {
        return status == ActionStatus.SUCCESS;
    }

    public static final class Builder {
        private final ActionType type;
        private final Map<Need, Integer> needDeltas = new EnumMap<>(Need.class);
        private int vitalityDelta = 0;
        private HealthCondition newCondition = null;
        private final Map<ItemType, Integer> inventoryDeltas = new EnumMap<>(ItemType.class);
        private int workContribution = 0;
        private RegionId migrationRequest = null;
        private final java.util.List<ProposedEvent> eventProposals = new java.util.ArrayList<>();
        private final List<ResourceClaim> resourceClaims = new ArrayList<>();

        private Builder(ActionType type) {
            this.type = Objects.requireNonNull(type, "type");
        }

        public Builder needDelta(Need need, int delta) {
            needDeltas.put(Objects.requireNonNull(need, "need"), delta);
            return this;
        }

        public Builder vitalityDelta(int delta) {
            this.vitalityDelta = delta;
            return this;
        }

        public Builder newCondition(HealthCondition condition) {
            this.newCondition = condition;
            return this;
        }

        public Builder inventoryDelta(ItemType item, int delta) {
            inventoryDeltas.put(Objects.requireNonNull(item, "item"), delta);
            return this;
        }

        public Builder workContribution(int contribution) {
            this.workContribution = contribution;
            return this;
        }

        public Builder migrationRequest(RegionId destination) {
            this.migrationRequest = destination;
            return this;
        }

        public Builder addEvent(ProposedEvent event) {
            eventProposals.add(Objects.requireNonNull(event, "event"));
            return this;
        }

        /** Add a resource claim to the result. */
        public Builder addResourceClaim(ResourceClaim claim) {
            resourceClaims.add(Objects.requireNonNull(claim, "claim"));
            return this;
        }

        public ActionResult build() {
            return new ActionResult(
                    type,
                    ActionStatus.SUCCESS,
                    Optional.empty(),
                    needDeltas,
                    vitalityDelta,
                    Optional.ofNullable(newCondition),
                    inventoryDeltas,
                    workContribution,
                    Optional.ofNullable(migrationRequest),
                    eventProposals,
                    resourceClaims
            );
        }
    }
}
