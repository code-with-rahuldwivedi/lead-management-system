package com.example.leads.controller;

import com.example.leads.exception.BusinessRuleException;
import com.example.leads.service.AssignmentService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/counsellors")
public class CounsellorController {

    private static final String ACTOR = "admin";

    private final AssignmentService assignmentService;

    public CounsellorController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @PostMapping("/{id}/unavailable")
    public String markUnavailable(@PathVariable Long id, RedirectAttributes ra) {
        try {
            int moved = assignmentService.deactivateAndRedistribute(id, ACTOR);
            ra.addFlashAttribute("success",
                    "Counsellor marked unavailable. " + moved + " open lead(s) redistributed.");
        } catch (BusinessRuleException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/";
    }
}