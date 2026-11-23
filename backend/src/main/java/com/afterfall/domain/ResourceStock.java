package com.afterfall.domain;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Settlement-level shared resource pool holding one {@link Resource} per {@link ResourceType}.
 *
 * <p>Thread-safety is delegated to each {@link Resource}'s internal lock.
 * The TickCoordinator is the sole writer; workers submit claims and never touch this directly.
 */
public final class ResourceStock {

    private final Map<ResourceType, Resource> resources;

    /**
     * Constructs a stock with the given initial quantities.
     *
     * @param food     initial food units
     * @param water    initial water units
     * @param medicine initial medicine units
     */
    public ResourceStock(int food, int water, int medicine) {
        Map<ResourceType, Resource> map = new EnumMap<>(ResourceType.class);
        map.put(ResourceType.FOOD,     new Resource(ResourceType.FOOD,     food));
        map.put(ResourceType.WATER,    new Resource(ResourceType.WATER,    water));
        map.put(ResourceType.MEDICINE, new Resource(ResourceType.MEDICINE, medicine));
        this.resources = Collections.unmodifiableMap(map);
    }

    /**
     * Default stock used when no quantities are specified.
     * Enough for a 25-survivor settlement to run a few ticks.
     */
    public static ResourceStock defaultStock() {
        return new ResourceStock(50, 50, 20);
    }

    public Resource resource(ResourceType type) {
        return resources.get(Objects.requireNonNull(type, "type"));
    }

    public int quantity(ResourceType type) {
        return resource(type).quantity();
    }

    public boolean isAvailable(ResourceType type) {
        return resource(type).isAvailable();
    }

    @Override
    public String toString() {
        return "ResourceStock{food=" + quantity(ResourceType.FOOD)
                + ", water=" + quantity(ResourceType.WATER)
                + ", medicine=" + quantity(ResourceType.MEDICINE) + "}";
    }
}
