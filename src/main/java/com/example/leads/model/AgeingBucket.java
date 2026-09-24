package com.example.leads.model;

public enum AgeingBucket {
    D0_2("0-2 days"),
    D3_7("3-7 days"),
    D8_14("8-14 days"),
    D15_PLUS("15+ days");

    private final String label;

    AgeingBucket(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static AgeingBucket of(long days) {
        if (days <= 2) return D0_2;
        if (days <= 7) return D3_7;
        if (days <= 14) return D8_14;
        return D15_PLUS;
    }
}