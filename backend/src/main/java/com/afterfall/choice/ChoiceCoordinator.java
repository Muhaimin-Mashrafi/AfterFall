package com.afterfall.choice;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Coordinates choice moments, validates player responses, falls back to defaults,
 * and queues consequences for deterministic execution at tick boundaries.
 */
public final class ChoiceCoordinator {

    private final Map<String, ChoiceMoment> activeMoments = new ConcurrentHashMap<>();
    private final Queue<ChoiceConsequence> pendingConsequences = new ConcurrentLinkedQueue<>();
    private final List<ChoiceRecord> decisionLog = new CopyOnWriteArrayList<>();
    private ChoiceMoment pendingPauseMoment = null;

    public ChoiceCoordinator() {
    }

    /**
     * Registers a new choice moment and flags that the simulation should pause
     * at the next safe boundary.
     */
    public synchronized void triggerChoice(ChoiceMoment moment) {
        Objects.requireNonNull(moment, "moment");
        activeMoments.put(moment.id(), moment);
        this.pendingPauseMoment = moment;
    }

    /**
     * Returns true if there is an unresolved choice moment requiring a simulation pause.
     */
    public synchronized boolean hasPendingPause() {
        return pendingPauseMoment != null && !pendingPauseMoment.isResolved();
    }

    public synchronized Optional<ChoiceMoment> pendingChoice() {
        return Optional.ofNullable(pendingPauseMoment);
    }

    /**
     * Submits a decision for an active choice moment.
     * If optionId is invalid or null, falls back safely to the moment's default option.
     */
    public synchronized ChoiceRecord submitDecision(String choiceId, String optionId) {
        ChoiceMoment moment = activeMoments.get(choiceId);
        if (moment == null) {
            throw new IllegalArgumentException("No active ChoiceMoment with id: " + choiceId);
        }
        if (moment.isResolved()) {
            throw new IllegalStateException("ChoiceMoment " + choiceId + " has already been resolved");
        }

        boolean usedDefault = false;
        ChoiceOption option;
        if (optionId != null && moment.findOption(optionId).isPresent()) {
            option = moment.resolve(optionId);
        } else {
            option = moment.resolveDefault();
            usedDefault = true;
        }

        // Queue consequence for execution at the next tick boundary
        pendingConsequences.offer(option.consequence());

        ChoiceRecord record = new ChoiceRecord(
                moment.id(),
                moment.tick(),
                moment.chapterId(),
                option.optionId(),
                option.label(),
                option.consequence().summary(),
                usedDefault
        );
        decisionLog.add(record);

        if (pendingPauseMoment != null && pendingPauseMoment.id().equals(choiceId)) {
            pendingPauseMoment = null;
        }

        return record;
    }

    /**
     * Drains all pending consequences to be applied by the sole-writer TickCoordinator.
     */
    public List<ChoiceConsequence> drainPendingConsequences() {
        List<ChoiceConsequence> consequences = new ArrayList<>();
        ChoiceConsequence c;
        while ((c = pendingConsequences.poll()) != null) {
            consequences.add(c);
        }
        return consequences;
    }

    public List<ChoiceRecord> decisionLog() {
        return Collections.unmodifiableList(decisionLog);
    }

    public Optional<ChoiceMoment> getChoiceMoment(String choiceId) {
        return Optional.ofNullable(activeMoments.get(choiceId));
    }
}
