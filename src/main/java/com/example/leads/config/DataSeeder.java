package com.example.leads.config;

import com.example.leads.model.*;
import com.example.leads.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final String[] FIRST = {"Aarav", "Diya", "Rohan", "Sneha", "Kiran", "Meera", "Arjun", "Priya", "Vikram", "Ananya"};
    private static final String[] LAST = {"Sharma", "Reddy", "Nair", "Gowda", "Patel", "Iyer", "Singh", "Rao"};
    private static final String[] LOST_REASONS = {"Chose another college", "Fees too high", "Not reachable", "Changed course plan"};

    private final LeadRepository leadRepository;
    private final CourseRepository courseRepository;
    private final CounsellorRepository counsellorRepository;
    private final FollowUpRepository followUpRepository;
    private final ActivityLogRepository activityLogRepository;
    private final boolean enabled;

    public DataSeeder(LeadRepository leadRepository, CourseRepository courseRepository,
                      CounsellorRepository counsellorRepository, FollowUpRepository followUpRepository,
                      ActivityLogRepository activityLogRepository,
                      @Value("${app.seed.enabled:true}") boolean enabled) {
        this.leadRepository = leadRepository;
        this.courseRepository = courseRepository;
        this.counsellorRepository = counsellorRepository;
        this.followUpRepository = followUpRepository;
        this.activityLogRepository = activityLogRepository;
        this.enabled = enabled;
    }

    @Override
    @Transactional
    public void run(String... args) {
        
        if (!enabled || leadRepository.count() > 0) {
            return;
        }

        List<Course> courses = courseRepository.saveAll(List.of(
                new Course("B.Tech Computer Science"), new Course("B.Tech Electronics"),
                new Course("MBA"), new Course("BBA"), new Course("B.Com"), new Course("MCA")));

        List<Counsellor> counsellors = counsellorRepository.saveAll(List.of(
                new Counsellor("Asha Menon", "asha@example.com", true),
                new Counsellor("Rahul Verma", "rahul@example.com", true),
                new Counsellor("Neha Joshi", "neha@example.com", true),
                new Counsellor("Suresh Kumar", "suresh@example.com", false))); // chhutti par: usko lead nahi milni chahiye

        List<Counsellor> active = counsellors.stream().filter(Counsellor::isActive).toList();
        LeadSource[] sources = LeadSource.values();
        LeadStatus[] statuses = LeadStatus.values();
        FollowUpType[] types = FollowUpType.values();
        Random rnd = new Random(42);
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < 40; i++) {
            Lead lead = new Lead();
            String first = FIRST[i % FIRST.length];
            String last = LAST[(i * 3) % LAST.length];
            lead.setName(first + " " + last);
            lead.setPhone(String.format("9%09d", 100_000_000 + i * 7919));   // har baar alag valid number
            lead.setEmail((first + "." + last + i + "@example.com").toLowerCase());
            lead.setSource(sources[rnd.nextInt(sources.length)]);
            lead.setCourseInterest(courses.get(rnd.nextInt(courses.size())));
            lead.setAssignedTo(active.get(i % active.size()));

            LocalDateTime created = now.minusDays(rnd.nextInt(45)).minusHours(rnd.nextInt(24));
            lead.setCreatedAt(created);

            LeadStatus status = statuses[rnd.nextInt(statuses.length)];
            lead.setStatus(status);
            if (status != LeadStatus.NEW) {
                LocalDateTime contacted = created.plusHours(1 + rnd.nextInt(72));
                lead.setLastContactedAt(contacted.isAfter(now) ? now : contacted);
            }
            if (status == LeadStatus.LOST) {
                lead.setLostReason(LOST_REASONS[rnd.nextInt(LOST_REASONS.length)]);
            }
            leadRepository.save(lead);

            activityLogRepository.save(new ActivityLog(lead, ActivityType.CREATED, "system",
                    "Lead created via " + lead.getSource().getLabel(), null, null));

            if (!status.isClosed()) {
                FollowUp f = new FollowUp();
                f.setLead(lead);
                f.setType(types[rnd.nextInt(types.length)]);
                f.setDueAt(now.plusDays(rnd.nextInt(7) - 3));   // kuch overdue, kuch aage ke
                f.setNote("Discuss course and fee structure");
                followUpRepository.save(f);
            }
        }
        log.info("Seeded demo data: {} courses, {} counsellors, {} leads",
                courseRepository.count(), counsellorRepository.count(), leadRepository.count());
    }
}