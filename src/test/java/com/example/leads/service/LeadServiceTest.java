package com.example.leads.service;

import com.example.leads.dto.CreateResult;
import com.example.leads.dto.LeadForm;
import com.example.leads.exception.BusinessRuleException;
import com.example.leads.model.ActivityType;
import com.example.leads.model.Counsellor;
import com.example.leads.model.Lead;
import com.example.leads.model.LeadSource;
import com.example.leads.model.LeadStatus;
import com.example.leads.repository.CounsellorRepository;
import com.example.leads.repository.LeadRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
class LeadServiceTest {

    @Autowired LeadService leadService;
    @Autowired AssignmentService assignmentService;
    @Autowired LeadRepository leadRepository;
    @Autowired CounsellorRepository counsellorRepository;

    private LeadForm form(String name, String phone) {
        LeadForm f = new LeadForm();
        f.setName(name);
        f.setPhone(phone);
        f.setSource(LeadSource.WEBSITE);
        return f;
    }

    private Lead newLead(String phone) {
        return leadService.createLead(form("Test Student", phone), "tester").getLead();
    }

    @Test
    void createsLeadAndAssignsAnActiveCounsellor() {
        Lead lead = newLead("9876543210");
        assertEquals(LeadStatus.NEW, lead.getStatus());
        assertNotNull(lead.getAssignedTo());
        assertTrue(lead.getAssignedTo().isActive());
    }

    @Test
    void neverAssignsToInactiveCounsellor() {
        for (int i = 0; i < 12; i++) {
            Lead lead = newLead(String.format("98765%05d", i));
            assertTrue(lead.getAssignedTo().isActive(), "lead " + i + " went to an inactive counsellor");
        }
    }

    @Test
    void duplicatePhoneInDifferentFormatDoesNotCreateSecondLead() {
        Lead first = newLead("98765 43210");
        long before = leadRepository.count();

        CreateResult second = leadService.createLead(form("Same Person", "+91 98765-43210"), "tester");

        assertTrue(second.isDuplicate());
        assertEquals(first.getId(), second.getLead().getId());
        assertEquals(before, leadRepository.count());
        assertTrue(leadService.timeline(first.getId()).stream()
                .anyMatch(a -> a.getType() == ActivityType.DUPLICATE_ENQUIRY));
    }

    @Test
    void invalidPhoneIsRejected() {
        assertThrows(BusinessRuleException.class, () -> leadService.createLead(form("Bad", "12345"), "tester"));
        assertThrows(BusinessRuleException.class, () -> leadService.createLead(form("Bad", "5876543210"), "tester"));
    }

    @Test
    void illegalStatusJumpIsRejected() {
        Lead lead = newLead("9876500001");
        assertThrows(BusinessRuleException.class,
                () -> leadService.changeStatus(lead.getId(), LeadStatus.ADMITTED, null, "tester"));
    }

    @Test
    void markingLostNeedsAReason() {
        Lead lead = newLead("9876500002");
        assertThrows(BusinessRuleException.class,
                () -> leadService.changeStatus(lead.getId(), LeadStatus.LOST, "   ", "tester"));
        leadService.changeStatus(lead.getId(), LeadStatus.LOST, "Fees too high", "tester");
        assertEquals(LeadStatus.LOST, leadService.get(lead.getId()).getStatus());
    }

    @Test
    void validTransitionUpdatesStatusContactTimeAndTimeline() {
        Lead lead = newLead("9876500003");
        leadService.changeStatus(lead.getId(), LeadStatus.CONTACTED, null, "tester");

        Lead reloaded = leadService.get(lead.getId());
        assertEquals(LeadStatus.CONTACTED, reloaded.getStatus());
        assertNotNull(reloaded.getLastContactedAt());
        assertTrue(leadService.timeline(lead.getId()).stream()
                .anyMatch(a -> a.getType() == ActivityType.STATUS_CHANGED));
    }

    @Test
    void closedLeadCannotBeReassigned() {
        Lead lead = newLead("9876500004");
        leadService.changeStatus(lead.getId(), LeadStatus.LOST, "Not reachable", "tester");
        Counsellor any = counsellorRepository.findByActiveTrueOrderByNameAsc().get(0);
        assertThrows(BusinessRuleException.class, () -> leadService.reassign(lead.getId(), any.getId(), "tester"));
    }

    @Test
    void cannotReassignToInactiveCounsellor() {
        Lead lead = newLead("9876500005");
        Counsellor inactive = counsellorRepository.findAll().stream()
                .filter(c -> !c.isActive()).findFirst().orElseThrow();
        assertThrows(BusinessRuleException.class, () -> leadService.reassign(lead.getId(), inactive.getId(), "tester"));
    }

    @Test
    void deactivatingCounsellorMovesAllTheirOpenLeads() {
        Counsellor victim = counsellorRepository.findByActiveTrueOrderByNameAsc().get(0);

        assignmentService.deactivateAndRedistribute(victim.getId(), "tester");

        assertTrue(leadRepository.findByAssignedToIdAndStatusNotIn(victim.getId(), LeadStatus.CLOSED).isEmpty());
        assertFalse(counsellorRepository.findById(victim.getId()).orElseThrow().isActive());
    }
}