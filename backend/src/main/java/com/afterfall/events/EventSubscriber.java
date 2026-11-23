package com.afterfall.events;

import java.util.List;

/**
 * Interface for independent subscribers listening to events fanned out by the {@link EventBus}.
 *
 * <p>Each subscriber runs with its own queue/buffer; a slow subscriber cannot block the
 * publisher or other independent subscribers.
 */
public interface EventSubscriber {

    /** Unique identifier for this subscriber (e.g. "narrator", "event-recorder"). */
    String subscriberId();

    /** Receives a batch of canonical simulation events in deterministic order. */
    void onEvents(List<SimEvent> events);
}
