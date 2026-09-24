package com.example.leads.model;

public enum FollowUpType {
    CALL("Call"),
    WHATSAPP("WhatsApp message"),
    EMAIL("Email"),
    CAMPUS_VISIT("Campus visit"),
    OTHER("Other");

    private final String label;

    FollowUpType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}