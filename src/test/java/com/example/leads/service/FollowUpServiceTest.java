package com.example.leads.service;

import com.example.leads.dto.LeadForm;
import com.example.leads.exception.BusinessRuleException;
import com.example.leads.model.FollowUp;
import com.example.leads.model.FollowUpType;
import com.example.leads.model.Lead;
import com.example.leads.model.LeadSource;
import com.example.leads.model.LeadStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
class FollowUpServiceTest {

    @Autowired LeadService leadService;
    @Autowired FollowUpService followUpService;

    private Lead newLead(String phone) {
        LeadForm f = new LeadForm();
        f.setName("Follow Up Student");
        f.setPhone(phone);
        f.setSource(LeadSource.PHONE);
        return leadService.createLead(f, "tester").getLead();
    }

    @Test
    void pastFollowUpIsRejected() {
        Lead lead = newLead("9876511111");
        assertThrows(BusinessRuleException.class, () -> followUpService.schedule(
                lead.getId(), LocalDateTime.now().minusDays(1), FollowUpType.CALL, null, "tester"));
    }

    @Test
    void missingDateIsRejected() {
        Lead lead = newLead("9876511112");
        assertThrows(BusinessRuleException.class, () -> followUpService.schedule(
                lead.getId(), null, FollowUpType.CALL, null, "tester"));
    }

    @Test
    void cannotScheduleOnClosedLead() {
        Lead lead = newLead("9876511113");
        leadService.changeStatus(lead.getId(), LeadStatus.LOST, "Changed plans", "tester");
        assertThrows(BusinessRuleException.class, () -> followUpService.schedule(
                lead.getId(), LocalDateTime.now().plusDays(1), FollowUpType.CALL, null, "tester"));
    }

    @Test
    void completingFollowUpResetsAgeingClockAndCannotBeRepeated() {
        Lead lead = newLead("9876511114");
        FollowUp f = followUpService.schedule(
                lead.getId(), LocalDateTime.now().plusHours(2), FollowUpType.WHATSAPP, "Send brochure", "tester");

        followUpService.complete(f.getId(), "tester");

        assertNotNull(leadService.get(lead.getId()).getLastContactedAt());
        assertThrows(BusinessRuleException.class, () -> followUpService.complete(f.getId(), "tester"));
    }
}