package com.example.leads.service;

import com.example.leads.dto.CreateResult;
import com.example.leads.dto.LeadForm;
import com.example.leads.exception.BusinessRuleException;
import com.example.leads.exception.ResourceNotFoundException;
import com.example.leads.model.ActivityLog;
import com.example.leads.model.ActivityType;
import com.example.leads.model.Counsellor;
import com.example.leads.model.Course;
import com.example.leads.model.Lead;
import com.example.leads.model.LeadSource;
import com.example.leads.model.LeadStatus;
import com.example.leads.repository.ActivityLogRepository;
import com.example.leads.repository.CounsellorRepository;
import com.example.leads.repository.CourseRepository;
import com.example.leads.repository.LeadRepository;
import com.example.leads.repository.LeadSpecs;
import com.example.leads.util.PhoneUtil;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class LeadService {

    private static final int MAX_REASON_LENGTH = 255;

    private final LeadRepository leadRepository;
    private final CourseRepository courseRepository;
    private final CounsellorRepository counsellorRepository;
    private final ActivityLogRepository activityLogRepository;
    private final AssignmentService assignmentService;

    public LeadService(LeadRepository leadRepository, CourseRepository courseRepository,
                       CounsellorRepository counsellorRepository,
                       ActivityLogRepository activityLogRepository,
                       AssignmentService assignmentService) {
        this.leadRepository = leadRepository;
        this.courseRepository = courseRepository;
        this.counsellorRepository = counsellorRepository;
        this.activityLogRepository = activityLogRepository;
        this.assignmentService = assignmentService;
    }

    // ------------------------------------------------------------ create

    @Transactional
    public CreateResult createLead(LeadForm form, String actor) {
        String phone = PhoneUtil.normalize(form.getPhone());
        if (!PhoneUtil.isValid(phone)) {
            throw new BusinessRuleException("Enter a valid 10-digit Indian mobile number");
        }

         Optional<Lead> existing = leadRepository.findByPhone(phone);
        if (existing.isPresent()) {
            Lead lead = existing.get();
            log(lead, ActivityType.DUPLICATE_ENQUIRY, actor,
                    "Repeat enquiry via " + form.getSource().getLabel() + " - no new lead created", null, null);
            return new CreateResult(lead, true);
        }

        Lead lead = new Lead();
        lead.setName(form.getName().trim());
        lead.setPhone(phone);
        lead.setEmail(blankToNull(form.getEmail()));
        lead.setSource(form.getSource());
        lead.setNotes(blankToNull(form.getNotes()));
        if (form.getCourseId() != null) {
            lead.setCourseInterest(courseRepository.findById(form.getCourseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + form.getCourseId())));
        }

        Counsellor counsellor = assignmentService.pickCounsellor();
        lead.setAssignedTo(counsellor);
        leadRepository.save(lead);

        log(lead, ActivityType.CREATED, actor, "Lead created via " + lead.getSource().getLabel(), null, null);
        if (counsellor != null) {
            log(lead, ActivityType.ASSIGNED, actor, "Auto-assigned (least open leads)", null, counsellor.getName());
        } else {
            log(lead, ActivityType.NOTE_ADDED, actor, "No active counsellor available - lead left unassigned", null, null);
        }
        return new CreateResult(lead, false);
    }

 
    @Transactional
    public void changeStatus(Long leadId, LeadStatus next, String reason, String actor) {
        if (next == null) {
            throw new BusinessRuleException("Select a status");
        }
        Lead lead = getOrThrow(leadId);
        LeadStatus current = lead.getStatus();

        if (!current.canMoveTo(next)) {
            throw new BusinessRuleException("Cannot move a lead from "
                    + current.getLabel() + " to " + next.getLabel());
        }

        String cleanReason = reason == null ? "" : reason.trim();
        if (next == LeadStatus.LOST) {
            if (cleanReason.isEmpty()) {
                throw new BusinessRuleException("A reason is required when marking a lead as Lost");
            }
            if (cleanReason.length() > MAX_REASON_LENGTH) {
                throw new BusinessRuleException("Reason must be at most " + MAX_REASON_LENGTH + " characters");
            }
            lead.setLostReason(cleanReason);
        }

        lead.setStatus(next);
        lead.setLastContactedAt(LocalDateTime.now());   // status badla = kisi ne lead par kaam kiya
        log(lead, ActivityType.STATUS_CHANGED, actor,
                next == LeadStatus.LOST ? "Lost: " + cleanReason : "Status changed",
                current.name(), next.name());
    }

 
    @Transactional
    public void reassign(Long leadId, Long counsellorId, String actor) {
        Lead lead = getOrThrow(leadId);
        if (lead.getStatus().isClosed()) {
            throw new BusinessRuleException("Closed leads cannot be reassigned");
        }
        if (counsellorId == null) {
            throw new BusinessRuleException("Select a counsellor");
        }
        Counsellor target = counsellorRepository.findById(counsellorId)
                .orElseThrow(() -> new ResourceNotFoundException("Counsellor not found: " + counsellorId));
        if (!target.isActive()) {
            throw new BusinessRuleException(target.getName() + " is unavailable and cannot receive leads");
        }
        Counsellor old = lead.getAssignedTo();
        if (old != null && old.getId().equals(target.getId())) {
            throw new BusinessRuleException("Lead is already assigned to " + target.getName());
        }
        lead.setAssignedTo(target);
        log(lead, ActivityType.REASSIGNED, actor, "Manually reassigned",
                old == null ? "Unassigned" : old.getName(), target.getName());
    }

 
    @Transactional(readOnly = true)
    public Lead get(Long id) {
        return getOrThrow(id);
    }

    @Transactional(readOnly = true)
    public List<Lead> search(LeadStatus status, LeadSource source, Long counsellorId, String q) {
        return leadRepository.findAll(LeadSpecs.filter(status, source, counsellorId, q),
                Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Transactional(readOnly = true)
    public List<ActivityLog> timeline(Long leadId) {
        return activityLogRepository.findByLeadIdOrderByCreatedAtDesc(leadId);
    }

    @Transactional(readOnly = true)
    public List<Course> activeCourses() {
        return courseRepository.findByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Counsellor> activeCounsellors() {
        return counsellorRepository.findByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Counsellor> allCounsellors() {
        return counsellorRepository.findAll(Sort.by("name"));
    }

 
    private Lead getOrThrow(Long id) {
        return leadRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found: " + id));
    }

    private void log(Lead lead, ActivityType type, String actor, String description, String oldV, String newV) {
        activityLogRepository.save(new ActivityLog(lead, type, actor, description, oldV, newV));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}