package com.example.leads.controller;

import com.example.leads.dto.CreateResult;
import com.example.leads.dto.LeadForm;
import com.example.leads.exception.BusinessRuleException;
import com.example.leads.model.FollowUpType;
import com.example.leads.model.Lead;
import com.example.leads.model.LeadSource;
import com.example.leads.model.LeadStatus;
import com.example.leads.service.FollowUpService;
import com.example.leads.service.LeadService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/leads")
public class LeadController {

    private static final String ACTOR = "admin";

    private final LeadService leadService;
    private final FollowUpService followUpService;

    public LeadController(LeadService leadService, FollowUpService followUpService) {
        this.leadService = leadService;
        this.followUpService = followUpService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) LeadStatus status,
                       @RequestParam(required = false) LeadSource source,
                       @RequestParam(required = false) Long counsellorId,
                       @RequestParam(required = false) String q,
                       Model model) {
        model.addAttribute("leads", leadService.search(status, source, counsellorId, q));
        model.addAttribute("statuses", LeadStatus.values());
        model.addAttribute("sources", LeadSource.values());
        model.addAttribute("counsellors", leadService.allCounsellors());
        model.addAttribute("fStatus", status);
        model.addAttribute("fSource", source);
        model.addAttribute("fCounsellorId", counsellorId);
        model.addAttribute("fQ", q);
        return "leads/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("leadForm", new LeadForm());
        addFormReferences(model);
        return "leads/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("leadForm") LeadForm form, BindingResult result,
                         Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            addFormReferences(model);
            return "leads/form";
        }
        try {
            CreateResult created = leadService.createLead(form, ACTOR);
            if (created.isDuplicate()) {
                ra.addFlashAttribute("warning",
                        "This phone number already exists. Logged as a repeat enquiry on the existing lead.");
            } else {
                ra.addFlashAttribute("success", "Lead created and assigned.");
            }
            return "redirect:/leads/" + created.getLead().getId();
        } catch (BusinessRuleException e) {
            result.reject("business", e.getMessage());
        } catch (DataIntegrityViolationException e) {
            result.reject("duplicate", "This phone number was just added by someone else. Please search for it.");
        }
        addFormReferences(model);
        return "leads/form";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Lead lead = leadService.get(id);
        model.addAttribute("lead", lead);
        model.addAttribute("nextStatuses", lead.getStatus().allowedNext());
        model.addAttribute("timeline", leadService.timeline(id));
        model.addAttribute("followUps", followUpService.forLead(id));
        model.addAttribute("counsellors", leadService.activeCounsellors());
        model.addAttribute("followUpTypes", FollowUpType.values());
        return "leads/detail";
    }

    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable Long id, @RequestParam LeadStatus status,
                               @RequestParam(required = false) String reason, RedirectAttributes ra) {
        try {
            leadService.changeStatus(id, status, reason, ACTOR);
            ra.addFlashAttribute("success", "Status updated to " + status.getLabel());
        } catch (BusinessRuleException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/leads/" + id;
    }

    @PostMapping("/{id}/assign")
    public String assign(@PathVariable Long id, @RequestParam(required = false) Long counsellorId,
                         RedirectAttributes ra) {
        try {
            leadService.reassign(id, counsellorId, ACTOR);
            ra.addFlashAttribute("success", "Lead reassigned.");
        } catch (BusinessRuleException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/leads/" + id;
    }

    @PostMapping("/{id}/followups")
    public String addFollowUp(@PathVariable Long id,
                              @RequestParam(required = false)
                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dueAt,
                              @RequestParam(required = false) FollowUpType type,
                              @RequestParam(required = false) String note,
                              RedirectAttributes ra) {
        try {
            followUpService.schedule(id, dueAt, type, note, ACTOR);
            ra.addFlashAttribute("success", "Follow-up scheduled.");
        } catch (BusinessRuleException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/leads/" + id;
    }

    private void addFormReferences(Model model) {
        model.addAttribute("sources", LeadSource.values());
        model.addAttribute("courses", leadService.activeCourses());
    }
}