package com.example.leads.model;

public enum LeadSource {
    WEBSITE("Website"),
    WALK_IN("Walk-in"),
    PHONE("Phone call"),
    WHATSAPP("WhatsApp"),
    FAIR("Education fair"),
    CAMPAIGN("Campaign"),
    REFERRAL("Referral"),
    OTHER("Other");

    private final String label;

    LeadSource(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}