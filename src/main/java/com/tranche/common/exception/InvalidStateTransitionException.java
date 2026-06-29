package com.tranche.common.exception;

import com.tranche.opportunity.domain.OpportunityStatus;

public class InvalidStateTransitionException extends BusinessException {

    public InvalidStateTransitionException(OpportunityStatus from, OpportunityStatus to) {
        super(
                ErrorCode.INVALID_STATE_TRANSITION,
                "Cannot transition opportunity from " + from + " to " + to
        );
    }
}
