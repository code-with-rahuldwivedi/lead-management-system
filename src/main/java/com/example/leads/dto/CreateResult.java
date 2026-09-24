package com.example.leads.dto;

import com.example.leads.model.Lead;

public class CreateResult {
    private final Lead lead;
    private final boolean duplicate;

    public CreateResult(Lead lead, boolean duplicate) {
        this.lead = lead;
        this.duplicate = duplicate;
    }

    public Lead getLead() {
    		return lead; 
    	}
    public boolean isDuplicate() { 
    		return duplicate;
    	}
}