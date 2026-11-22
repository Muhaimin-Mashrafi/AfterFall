
package com.afterfall.action;

import com.afterfall.domain.ResourceType;
import java.util.Objects;

/**
 * Immutable claim for a shared settlement resource.
 *
 * A claim proposes consumption but does not mutate resource storage.
 * It will be resolved deterministically by the future TickCoordinator.
 */
public record ResourceClaim(ResourceType resourceType, int quantity) {

    public ResourceClaim {
        Objects.requireNonNull(resourceType, "resourceType");

        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "quantity must be positive"
            );
        }
    }
}
