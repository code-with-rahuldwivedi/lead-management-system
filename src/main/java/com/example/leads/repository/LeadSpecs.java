package com.example.leads.repository;

import com.example.leads.model.Lead;
import com.example.leads.model.LeadSource;
import com.example.leads.model.LeadStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class LeadSpecs {

    private LeadSpecs() { }

    public static Specification<Lead> filter(LeadStatus status, LeadSource source,
                                             Long counsellorId, String q) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            if (source != null) predicates.add(cb.equal(root.get("source"), source));
            if (counsellorId != null) predicates.add(cb.equal(root.get("assignedTo").get("id"), counsellorId));
            if (q != null && !q.isBlank()) {
                String text = q.trim();
                String like = "%" + text.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("email")), like),
                        cb.like(root.get("phone"), "%" + text + "%")));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}