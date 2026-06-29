package com.tranche.opportunity.domain;

import com.tranche.common.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpportunityStateMachineTest {

    private final OpportunityStateMachine stateMachine = new OpportunityStateMachine();

    @Test
    void allowsFullHappyPath() {
        assertTransition(OpportunityStatus.DRAFT, OpportunityStatus.UNDER_REVIEW);
        assertTransition(OpportunityStatus.UNDER_REVIEW, OpportunityStatus.APPROVED);
        assertTransition(OpportunityStatus.APPROVED, OpportunityStatus.LIVE);
        assertTransition(OpportunityStatus.LIVE, OpportunityStatus.FULLY_SUBSCRIBED);
        assertTransition(OpportunityStatus.FULLY_SUBSCRIBED, OpportunityStatus.MATURED);
        assertTransition(OpportunityStatus.MATURED, OpportunityStatus.SETTLED);
    }

    @Test
    void allowsAdminRejectAndManualMaturityPaths() {
        assertTransition(OpportunityStatus.UNDER_REVIEW, OpportunityStatus.DRAFT);
        assertTransition(OpportunityStatus.LIVE, OpportunityStatus.MATURED);
    }

    @ParameterizedTest
    @EnumSource(OpportunityStatus.class)
    void settledIsTerminal(OpportunityStatus target) {
        assertThat(stateMachine.canTransition(OpportunityStatus.SETTLED, target)).isFalse();
    }

    @Test
    void rejectsDraftToLive() {
        assertInvalidTransition(OpportunityStatus.DRAFT, OpportunityStatus.LIVE);
    }

    @Test
    void rejectsLiveToSettled() {
        assertInvalidTransition(OpportunityStatus.LIVE, OpportunityStatus.SETTLED);
    }

    @Test
    void rejectsApprovedToMatured() {
        assertInvalidTransition(OpportunityStatus.APPROVED, OpportunityStatus.MATURED);
    }

    @Test
    void rejectsMaturedToLive() {
        assertInvalidTransition(OpportunityStatus.MATURED, OpportunityStatus.LIVE);
    }

    private void assertTransition(OpportunityStatus from, OpportunityStatus to) {
        assertThatCode(() -> stateMachine.assertTransition(from, to)).doesNotThrowAnyException();
        assertThat(stateMachine.canTransition(from, to)).isTrue();
    }

    private void assertInvalidTransition(OpportunityStatus from, OpportunityStatus to) {
        assertThatThrownBy(() -> stateMachine.assertTransition(from, to))
                .isInstanceOf(InvalidStateTransitionException.class);
        assertThat(stateMachine.canTransition(from, to)).isFalse();
    }
}
