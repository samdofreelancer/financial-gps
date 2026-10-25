package com.financialgps.domain.gps;

import java.util.Objects;

/**
 * Route context: states the applied route/ordering policy.
 * MVP: single destination, Available Capacity directed to selected goal.
 */
public final class GpsRouteContext {

    private GpsRouteContext() {
    }

    public record Context(
            String policy,
            String description
    ) {
        public Context {
            Objects.requireNonNull(policy, "policy");
            Objects.requireNonNull(description, "description");
        }
    }

    public static final String POLICY_SINGLE_DESTINATION = "SINGLE_DESTINATION";

    public static Context singleDestination() {
        return new Context(
                POLICY_SINGLE_DESTINATION,
                "Available capacity directed to the single selected destination; "
                        + "multi-destination allocation not applied (owned by Feature 008)");
    }
}