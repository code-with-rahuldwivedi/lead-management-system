package com.example.leads.service;

import com.example.leads.exception.BusinessRuleException;
import com.example.leads.exception.ResourceNotFoundException;
import com.example.leads.model.ActivityLog;
import com.example.leads.model.ActivityType;
import com.example.leads.model.FollowUp;
import com.example.leads.model.FollowUpType;
import com.example.leads.model.Lead;
import com.example.leads.model.LeadStatus;
import com.example.leads.repository.ActivityLogRepository;
import com.example.leads.repository.FollowUpRepository;
import com.example.leads.repository.LeadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class FollowUpService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
    private static final int MAX_NOTE_LENGTH = 500;

    private final FollowUpRepository followUpRepository;
    private final LeadRepository leadRepository;
    private final ActivityLogRepository activityLogRepository;

    public FollowUpService(FollowUpRepository followUpRepository, LeadRepository leadRepository,
                           ActivityLogRepository activityLogRepository) {
        this.followUpRepository = followUpRepository;
        this.leadRepository = leadRepository;
        this.activityLogRepository = activityLogRepository;
    }

    @Transactional
    public FollowUp schedule(Long leadId, LocalDateTime dueAt, FollowUpType type, String note, String actor) {
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found: " + leadId));

        if (lead.getStatus().isClosed()) {
            throw new BusinessRuleException("Cannot schedule a follow-up on a closed lead");
        }
        if (dueAt == null) {
            throw new BusinessRuleException("Choose a follow-up date and time");
        }
        if (dueAt.isBefore(LocalDateTime.now().minusMinutes(1))) {   // 1 min ki chhoot (form slow submit)
            throw new BusinessRuleException("Follow-up time cannot be in the past");
        }
        String cleanNote = note == null || note.isBlank() ? null : note.trim();
        if (cleanNote != null && cleanNote.length() > MAX_NOTE_LENGTH) {
            throw new BusinessRuleException("Note must be at most " + MAX_NOTE_LENGTH + " characters");
        }

        FollowUp followUp = new FollowUp();
        followUp.setLead(lead);
        followUp.setDueAt(dueAt);
        followUp.setType(type == null ? FollowUpType.CALL : type);
        followUp.setNote(cleanNote);
        followUpRepository.save(followUp);

        activityLogRepository.save(new ActivityLog(lead, ActivityType.FOLLOW_UP_ADDED, actor,
                followUp.getType().getLabel() + " scheduled for " + dueAt.format(FMT), null, null));
        return followUp;
    }

    /** Follow-up complete = asli contact hua, isliye lead ka ageing clock reset. */
    @Transactional
    public void complete(Long followUpId, String actor) {
        FollowUp followUp = followUpRepository.findById(followUpId)
                .orElseThrow(() -> new ResourceNotFoundException("Follow-up not found: " + followUpId));
        if (followUp.isDone()) {
            throw new BusinessRuleException("This follow-up is already completed");
        }

        LocalDateTime now = LocalDateTime.now();
        followUp.setDone(true);
        followUp.setCompletedAt(now);

        Lead lead = followUp.getLead();
        lead.setLastContactedAt(now);
        activityLogRepository.save(new ActivityLog(lead, ActivityType.FOLLOW_UP_COMPLETED, actor,
                followUp.getType().getLabel() + " follow-up completed", null, null));
    }

    @Transactional(readOnly = true)
    public List<FollowUp> forLead(Long leadId) {
        return followUpRepository.findByLeadIdOrderByDueAtAsc(leadId);
    }

    @Transactional(readOnly = true)
    public List<FollowUp> overdue() {
        return followUpRepository.findOverdue(LocalDateTime.now(), LeadStatus.CLOSED);
    }
}