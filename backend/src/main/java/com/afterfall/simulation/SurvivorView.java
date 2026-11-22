package com.afterfall.simulation;

import com.afterfall.domain.*;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Optional;
import java.util.Optional;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable view of a Survivor for a tick.
 */
public record SurvivorView(
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
    public SurvivorView {
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

    public static SurvivorView from(Survivor survivor) {
        Objects.requireNonNull(survivor, "survivor");
        Map<Need, Integer> needs = new EnumMap<>(Need.class);
        for (Need n : Need.values()) {
            needs.put(n, survivor.needs().level(n));
        }
        Map<ItemType, Integer> items = new EnumMap<>(ItemType.class);
        for (ItemType it : ItemType.values()) {
            items.put(it, survivor.inventory().quantity(it));
        }
        return new SurvivorView(
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
        return needLevels.getOrDefault(Objects.requireNonNull(need), 0);
    }

    public int itemQuantity(ItemType type) {
        return inventoryQuantities.getOrDefault(Objects.requireNonNull(type), 0);
    }

    public boolean isAlive() {
        return status == SurvivorStatus.ALIVE;
    }
}
