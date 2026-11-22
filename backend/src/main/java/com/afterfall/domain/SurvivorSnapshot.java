package com.afterfall.domain;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * An immutable, read-only snapshot of a {@link Survivor}'s state for decision-making
 * and action evaluation. Defensively copies all internal collections and fields so workers
 * and behaviors cannot mutate domain entities during evaluation.
 */
public record SurvivorSnapshot(
        SurvivorId id,
        String name,
        Personality personality,
        Profession profession,
        Map<Need, Integer> needLevels,
        int vitality,
        HealthCondition condition,
        Map<ItemType, Integer> inventoryQuantities,
        int inventoryCapacity,
        Optional<RegionId> location,
        SurvivorStatus status
) {

    public SurvivorSnapshot {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(personality, "personality");
        Objects.requireNonNull(profession, "profession");
        Objects.requireNonNull(needLevels, "needLevels");
        Objects.requireNonNull(condition, "condition");
        Objects.requireNonNull(inventoryQuantities, "inventoryQuantities");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(status, "status");
        needLevels = Collections.unmodifiableMap(new EnumMap<>(needLevels));
        inventoryQuantities = Collections.unmodifiableMap(new EnumMap<>(inventoryQuantities));
    }

    public static SurvivorSnapshot from(Survivor survivor) {
        Objects.requireNonNull(survivor, "survivor");

        Map<Need, Integer> needs = new EnumMap<>(Need.class);
        for (Need need : Need.values()) {
            needs.put(need, survivor.needs().level(need));
        }

        Map<ItemType, Integer> items = new EnumMap<>(ItemType.class);
        for (ItemType type : ItemType.values()) {
            items.put(type, survivor.inventory().quantity(type));
        }

        return new SurvivorSnapshot(
                survivor.id(),
                survivor.name(),
                survivor.personality(),
                survivor.profession(),
                needs,
                survivor.health().vitality(),
                survivor.health().condition(),
                items,
                survivor.inventory().capacity(),
                survivor.location(),
                survivor.status()
        );
    }

    public int needLevel(Need need) {
        return needLevels.getOrDefault(Objects.requireNonNull(need, "need"), 0);
    }

    public int itemQuantity(ItemType type) {
        return inventoryQuantities.getOrDefault(Objects.requireNonNull(type, "type"), 0);
    }

    public boolean hasItem(ItemType type, int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must be non-negative");
        }
        return itemQuantity(type) >= amount;
    }

    public boolean isAlive() {
        return status == SurvivorStatus.ALIVE;
    }
}
