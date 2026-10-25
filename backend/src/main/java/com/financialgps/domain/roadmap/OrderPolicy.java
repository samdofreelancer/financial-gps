package com.financialgps.domain.roadmap;

import java.util.List;
import java.util.Objects;

/**
 * The order policy applied to the route, disclosed so the visible order is never a hidden truth
 * (spec 005 §7.2).
 *
 * <p>{@code scope} is load-bearing: {@link #SCOPE_ROADMAP_STAGE_ORDER} states that the policy
 * orders <b>stages</b>, not money. It is not the per-period allocation order of
 * {@code calculation-rules.md} §8, which Feature 008 owns.
 *
 * <p>{@code appliedOverrides} is empty in v1 — Feature 008 introduces real overrides.
 */
public record OrderPolicy(
        String policy,
        String scope,
        String description,
        List<String> appliedOverrides) {

    public static final String DEFAULT_POLICY = "DEBT_THEN_EMERGENCY_FUND_THEN_PRIORITY";
    public static final String SCOPE_ROADMAP_STAGE_ORDER = "ROADMAP_STAGE_ORDER";
    public static final String DEFAULT_DESCRIPTION =
            "Roadmap stage order: debt elimination, then emergency-fund goals, then the remaining "
                    + "goals by priority. This is not the allocation order.";

    public OrderPolicy {
        Objects.requireNonNull(policy, "policy");
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(appliedOverrides, "appliedOverrides");
        appliedOverrides = List.copyOf(appliedOverrides);
    }

    /** The single v1 policy: stage order only, no overrides. */
    public static OrderPolicy defaults() {
        return new OrderPolicy(DEFAULT_POLICY, SCOPE_ROADMAP_STAGE_ORDER, DEFAULT_DESCRIPTION,
                List.of());
    }
}
