# AfterFall - Approved Technical Clarifications

These clarifications refine Revision 2 without expanding its feature scope. They are binding implementation constraints alongside `CLAUDE.md`.

## 1. Actions must be evaluated, not applied by workers

A worker must not call a method that mutates shared world state. Prefer a pure evaluation contract:

```java
public interface Action {
    ActionResult evaluate(ActionContext context);
}
```

`ActionResult` should contain proposed survivor state deltas, resource claims, relationship deltas, migration requests and immutable events. The exact value types can be finalized in M1/M2, but the worker/merger boundary must remain.

## 2. RegionResult and single-writer merge

Each region task returns an immutable `RegionResult`. `TickCoordinator` is the only component that applies results to authoritative world state. Merge order is region ID, survivor ID and per-survivor action sequence. A worker failure prevents partial merge; fail or pause the tick cleanly.

## 3. Observer timing must be deterministic

ObserverAI analyses a completed immutable snapshot. Each intervention receives an explicit application tick and deterministic sequence. It is queued and applied by TickCoordinator at that boundary. Observer thread completion timing must not alter the run.

## 4. Relationships across regions

Workers return `RelationshipDelta` values. They do not mutate shared relationship objects. TickCoordinator validates and applies the deltas in a stable order during merge.

## 5. Chapter and choice timing

Evaluate chapter policies and critical-choice triggers at a deterministic post-merge boundary. Narrator may render the resulting scene asynchronously, but its thread scheduling must not decide when the simulation changes chapter or pauses.

## 6. Persistence queue guarantees

A bounded queue cannot be both lossless, always non-blocking and tolerant of indefinite database failure. Therefore:

- Critical events are never intentionally dropped.
- Minor events may be dropped or coalesced from a bounded queue; count drops in metrics.
- If the critical queue reaches its configured high-water mark, request a pause at the next safe tick boundary.
- If PostgreSQL fails, switch to the in-memory adapter and report that this fallback is process-local and not crash-durable.
- Flush critical events on orderly shutdown.

## 7. Resource claims

Workers submit claims; they do not consume shared resources during parallel evaluation. TickCoordinator resolves claims in a deterministic order and calls the Resource API. Resource itself still protects its non-negative quantity invariant. Keep the allocation rule explicit and test it. Do not describe region-ID/survivor-ID ordering as equitable unless an actual fairness policy is implemented.

## 8. External command ordering

Every outside input is validated, assigned an application tick and deterministic sequence, then applied at a tick boundary. Store the applied command/decision log so a run can be reproduced.

## 9. Audience voting (SHOULD only)

A browser session token can enforce one vote per browser session, not one vote per real person. A changed vote may replace the previous vote before the deadline. Tie-break and zero-vote rules must be deterministic. Operator override and audience majority must enter the simulation through the same command path.

## 10. Content and narrative integrity

The tragic ending is implied and non-graphic. Narrator templates must not name or depict the most sensitive acts explicitly. Captions must cite source events; the final recap must derive from stored run history. Observer claims must remain distinguishable from verified simulation facts.
