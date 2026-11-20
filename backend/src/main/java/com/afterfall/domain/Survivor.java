package com.afterfall.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * A person in the settlement, built by <em>composition</em> of Needs, Health, Personality,
 * Inventory and a Profession role object, plus relationships and memories. Profession is a field,
 * not a subclass, so a survivor can change role without changing type.
 *
 * <p>Location assignment ({@link #location()}) is managed centrally and authoritatively by
 * {@link Settlement#registerSurvivor(Survivor, RegionId)}; newly instantiated survivors begin with
 * an empty location. Identity is the {@link SurvivorId}. Components are exposed so the single
 * writer (the future TickCoordinator) can apply state changes; this class guards the invariants it
 * owns (identity, name, chronological memory ordering, no self-relationships, one relationship per
 * target).
 */
public final class Survivor {

    private final SurvivorId id;
    private final String name;
    private final Personality personality;
    private final Needs needs;
    private final Health health;
    private final Inventory inventory;
    private Profession profession;
    private RegionId location;
    private SurvivorStatus status = SurvivorStatus.ALIVE;
    // TreeMap: iteration in SurvivorId order, so anything built from it is deterministic.
    private final Map<SurvivorId, Relationship> relationships = new TreeMap<>();
    private final List<CharacterMemory> memories = new ArrayList<>();

    public Survivor(SurvivorId id, String name, Personality personality, Profession profession,
                    Needs needs, Health health, Inventory inventory) {
        this.id = Objects.requireNonNull(id, "id");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        this.name = name.strip();
        this.personality = Objects.requireNonNull(personality, "personality");
        this.profession = Objects.requireNonNull(profession, "profession");
        this.needs = Objects.requireNonNull(needs, "needs");
        this.health = Objects.requireNonNull(health, "health");
        this.inventory = Objects.requireNonNull(inventory, "inventory");
        this.location = null;
    }

    public SurvivorId id() {
        return id;
    }

    public String name() {
        return name;
    }

    public Personality personality() {
        return personality;
    }

    public Needs needs() {
        return needs;
    }

    public Health health() {
        return health;
    }

    public Inventory inventory() {
        return inventory;
    }

    public Profession profession() {
        return profession;
    }

    public void changeProfession(Profession newProfession) {
        this.profession = Objects.requireNonNull(newProfession, "newProfession");
    }

    public Optional<RegionId> location() {
        return Optional.ofNullable(location);
    }

    /** Package-private: location assignment is managed centrally through Settlement. */
    void setLocation(RegionId location) {
        this.location = location;
    }

    public SurvivorStatus status() {
        return status;
    }

    public boolean isAlive() {
        return status == SurvivorStatus.ALIVE;
    }

    /** Marks the survivor dead. Death is permanent; there is no way back. */
    public void markDead() {
        status = SurvivorStatus.DEAD;
    }

    /** Adds a relationship. Rejects self-relationships and a second one for the same target. */
    public void addRelationship(Relationship relationship) {
        Objects.requireNonNull(relationship, "relationship");
        if (relationship.target().equals(id)) {
            throw new IllegalArgumentException("A survivor cannot have a relationship with itself");
        }
        if (relationships.containsKey(relationship.target())) {
            throw new IllegalStateException(
                    "Relationship with " + relationship.target() + " already exists");
        }
        relationships.put(relationship.target(), relationship);
    }

    public Optional<Relationship> relationshipWith(SurvivorId other) {
        return Optional.ofNullable(relationships.get(other));
    }

    /** Unmodifiable view, ordered by target id. */
    public Collection<Relationship> relationships() {
        return Collections.unmodifiableCollection(relationships.values());
    }

    public void remember(CharacterMemory memory) {
        Objects.requireNonNull(memory, "memory");
        if (memory.relatedSurvivor().filter(id::equals).isPresent()) {
            throw new IllegalArgumentException("A memory cannot relate a survivor to itself");
        }
        if (!memories.isEmpty()) {
            int lastDay = memories.get(memories.size() - 1).day();
            if (memory.day() < lastDay) {
                throw new IllegalArgumentException(
                        "memory day " + memory.day() + " must not be earlier than last stored memory day " + lastDay);
            }
        }
        memories.add(memory);
    }

    /** Unmodifiable snapshot, oldest first. */
    public List<CharacterMemory> memories() {
        return List.copyOf(memories);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Survivor other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
