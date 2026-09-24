package com.example.leads.service;

import com.example.leads.exception.BusinessRuleException;
import com.example.leads.exception.ResourceNotFoundException;
import com.example.leads.model.ActivityLog;
import com.example.leads.model.ActivityType;
import com.example.leads.model.Counsellor;
import com.example.leads.model.Lead;
import com.example.leads.model.LeadStatus;
import com.example.leads.repository.ActivityLogRepository;
import com.example.leads.repository.CounsellorRepository;
import com.example.leads.repository.LeadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class AssignmentService {

    private final CounsellorRepository counsellorRepository;
    private final LeadRepository leadRepository;
    private final ActivityLogRepository activityLogRepository;

    public AssignmentService(CounsellorRepository counsellorRepository,
                             LeadRepository leadRepository,
                             ActivityLogRepository activityLogRepository) {
        this.counsellorRepository = counsellorRepository;
        this.leadRepository = leadRepository;
        this.activityLogRepository = activityLogRepository;
    }

    /** Sabse kam open leads wala active counsellor. Koi active na ho to null. */
    @Transactional
    public Counsellor pickCounsellor() {
        return counsellorRepository.findByActiveTrueOrderByNameAsc().stream()
                .min(Comparator
                        .comparingLong((Counsellor c) ->
                                leadRepository.countOpenByCounsellor(c.getId(), LeadStatus.CLOSED))
                        .thenComparing(Counsellor::getId))   // barabari ho to chhota id
                .orElse(null);
    }

    /** Counsellor ko unavailable karke uske OPEN leads baaki active counsellors mein baant deta hai. */
    @Transactional
    public int deactivateAndRedistribute(Long counsellorId, String actor) {
        Counsellor counsellor = counsellorRepository.findById(counsellorId)
                .orElseThrow(() -> new ResourceNotFoundException("Counsellor not found: " + counsellorId));

        if (!counsellor.isActive()) {
            throw new BusinessRuleException(counsellor.getName() + " is already unavailable");
        }
        if (counsellorRepository.findByActiveTrueOrderByNameAsc().size() <= 1) {
            throw new BusinessRuleException("Cannot mark the last active counsellor as unavailable");
        }

        counsellor.setActive(false);
        counsellorRepository.saveAndFlush(counsellor);

        List<Lead> openLeads = leadRepository.findByAssignedToIdAndStatusNotIn(counsellorId, LeadStatus.CLOSED);
        for (Lead lead : openLeads) {
            Counsellor target = pickCounsellor();   // har baar dobara: isse barabar baantwara hota hai
            lead.setAssignedTo(target);
            activityLogRepository.save(new ActivityLog(lead, ActivityType.REASSIGNED, actor,
                    counsellor.getName() + " unavailable - auto reassigned",
                    counsellor.getName(), target.getName()));
        }
        return openLeads.size();
    }
}