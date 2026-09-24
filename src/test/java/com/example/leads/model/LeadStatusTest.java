package com.example.leads.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeadStatusTest {

    @Test
    void validForwardTransitionsAreAllowed() {
        assertTrue(LeadStatus.NEW.canMoveTo(LeadStatus.CONTACTED));
        assertTrue(LeadStatus.APPLICATION_SUBMITTED.canMoveTo(LeadStatus.ADMITTED));
    }

    @Test
    void skippingStagesIsRejected() {
        assertFalse(LeadStatus.NEW.canMoveTo(LeadStatus.ADMITTED));
    }

    @Test
    void closedStatesAreTerminal() {
        assertTrue(LeadStatus.ADMITTED.allowedNext().isEmpty());
        assertFalse(LeadStatus.LOST.canMoveTo(LeadStatus.NEW));
    }
}