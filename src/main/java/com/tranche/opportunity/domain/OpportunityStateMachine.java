package com.tranche.opportunity.domain;

import com.tranche.common.exception.InvalidStateTransitionException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Enforces the opportunity lifecycle graph. All status changes must pass through here.
 */
@Component
public class OpportunityStateMachine {

    private final Map<OpportunityStatus, Set<OpportunityStatus>> allowedTransitions;

    public OpportunityStateMachine() {
        allowedTransitions = new EnumMap<>(OpportunityStatus.class);
        allowedTransitions.put(OpportunityStatus.DRAFT, EnumSet.of(OpportunityStatus.UNDER_REVIEW));
        allowedTransitions.put(OpportunityStatus.UNDER_REVIEW, EnumSet.of(
                OpportunityStatus.APPROVED,
                OpportunityStatus.DRAFT
        ));
        allowedTransitions.put(OpportunityStatus.APPROVED, EnumSet.of(OpportunityStatus.LIVE));
        allowedTransitions.put(OpportunityStatus.LIVE, EnumSet.of(
                OpportunityStatus.FULLY_SUBSCRIBED,
                OpportunityStatus.MATURED
        ));
        allowedTransitions.put(OpportunityStatus.FULLY_SUBSCRIBED, EnumSet.of(OpportunityStatus.MATURED));
        allowedTransitions.put(OpportunityStatus.MATURED, EnumSet.of(OpportunityStatus.SETTLED));
        allowedTransitions.put(OpportunityStatus.SETTLED, EnumSet.noneOf(OpportunityStatus.class));
    }

    public void assertTransition(OpportunityStatus from, OpportunityStatus to) {
        Set<OpportunityStatus> targets = allowedTransitions.getOrDefault(from, Set.of());
        if (!targets.contains(to)) {
            throw new InvalidStateTransitionException(from, to);
        }
    }

    public boolean canTransition(OpportunityStatus from, OpportunityStatus to) {
        return allowedTransitions.getOrDefault(from, Set.of()).contains(to);
    }
}
