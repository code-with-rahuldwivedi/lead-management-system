package com.example.leads.controller;

import com.example.leads.exception.BusinessRuleException;
import com.example.leads.service.FollowUpService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/followups")
public class FollowUpController {

    private static final String ACTOR = "admin";

    private final FollowUpService followUpService;

    public FollowUpController(FollowUpService followUpService) {
        this.followUpService = followUpService;
    }

    @GetMapping("/overdue")
    public String overdue(Model model) {
        model.addAttribute("followUps", followUpService.overdue());
        return "followups/overdue";
    }

    @PostMapping("/{id}/complete")
    public String complete(@PathVariable Long id,
                           @RequestParam(required = false) Long leadId,
                           @RequestParam(defaultValue = "lead") String from,
                           RedirectAttributes ra) {
        try {
            followUpService.complete(id, ACTOR);
            ra.addFlashAttribute("success", "Follow-up marked as done.");
        } catch (BusinessRuleException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        if ("overdue".equals(from) || leadId == null) {
            return "redirect:/followups/overdue";
        }
        return "redirect:/leads/" + leadId;
    }
}