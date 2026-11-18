/**
 * Simulation core: world clock, tick coordinator, region workers, ordered merge,
 * snapshots and deterministic RNG.
 *
 * <p>Plain Java only. No Spring, Spring MVC, WebSocket, JPA or PostgreSQL imports.
 * TickCoordinator is the single writer of authoritative world state.
 * Implemented from M3/M4 onward.
 */
package com.afterfall.simulation;
