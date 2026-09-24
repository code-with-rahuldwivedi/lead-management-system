package com.example.leads.model;

import java.util.EnumSet;
import java.util.Set;

public enum LeadStatus {
    NEW("New"),
    CONTACTED("Contacted"),
    INTERESTED("Interested"),
    APPLICATION_SUBMITTED("Application Submitted"),
    ADMITTED("Admitted"),
    LOST("Lost");

    public static final Set<LeadStatus> CLOSED = EnumSet.of(ADMITTED, LOST);

    private final String label;

    LeadStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public Set<LeadStatus> allowedNext() {
        return switch (this) {
            case NEW -> EnumSet.of(CONTACTED, LOST);
            case CONTACTED -> EnumSet.of(INTERESTED, LOST);
            case INTERESTED -> EnumSet.of(APPLICATION_SUBMITTED, LOST);
            case APPLICATION_SUBMITTED -> EnumSet.of(ADMITTED, LOST);
            case ADMITTED, LOST -> EnumSet.noneOf(LeadStatus.class);
        };
    }

    public boolean canMoveTo(LeadStatus next) {
        return next != null && allowedNext().contains(next);
    }

    public boolean isClosed() {
        return CLOSED.contains(this);
    }
}