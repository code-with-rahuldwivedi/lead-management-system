package com.example.leads.service;

import com.example.leads.model.AgeingBucket;
import com.example.leads.model.Counsellor;
import com.example.leads.model.Lead;
import com.example.leads.model.LeadSource;
import com.example.leads.model.LeadStatus;
import com.example.leads.repository.CounsellorRepository;
import com.example.leads.repository.FollowUpRepository;
import com.example.leads.repository.LeadRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private final LeadRepository leadRepository;
    private final CounsellorRepository counsellorRepository;
    private final FollowUpRepository followUpRepository;

    public ReportService(LeadRepository leadRepository, CounsellorRepository counsellorRepository,
                         FollowUpRepository followUpRepository) {
        this.leadRepository = leadRepository;
        this.counsellorRepository = counsellorRepository;
        this.followUpRepository = followUpRepository;
    }

    // ---- view classes ----

    public static class StatRow {
        private final String label;
        private final long count;
        private final long secondary;
        private final double pct;

        public StatRow(String label, long count, long secondary, double pct) {
            this.label = label;
            this.count = count;
            this.secondary = secondary;
            this.pct = pct;
        }

        public String getLabel() { return label; }
        public long getCount() { return count; }
        public long getSecondary() { return secondary; }
        public double getPct() { return pct; }
    }

    public static class WorkloadRow {
        private final Long id;
        private final String name;
        private final boolean active;
        private final long open;
        private final long admitted;

        public WorkloadRow(Long id, String name, boolean active, long open, long admitted) {
            this.id = id;
            this.name = name;
            this.active = active;
            this.open = open;
            this.admitted = admitted;
        }

        public Long getId() { return id; }
        public String getName() { return name; }
        public boolean isActive() { return active; }
        public long getOpen() { return open; }
        public long getAdmitted() { return admitted; }
    }

    // ---- reports ----

    public Map<String, Object> kpis() {
        Map<LeadStatus, Long> counts = statusCounts();
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        long admitted = counts.get(LeadStatus.ADMITTED);
        long lost = counts.get(LeadStatus.LOST);

        Map<String, Object> kpi = new LinkedHashMap<>();
        kpi.put("total", total);
        kpi.put("open", total - admitted - lost);
        kpi.put("admitted", admitted);
        kpi.put("lost", lost);
        kpi.put("conversion", percent(admitted, total));
        kpi.put("overdue", followUpRepository.countOverdue(LocalDateTime.now(), LeadStatus.CLOSED));
        return kpi;
    }

     public List<StatRow> funnel() {
        Map<LeadStatus, Long> counts = statusCounts();
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        List<StatRow> rows = new ArrayList<>();
        for (LeadStatus s : LeadStatus.values()) {
            long c = counts.get(s);
            rows.add(new StatRow(s.getLabel(), c, 0, percent(c, total)));
        }
        return rows;
    }

     public List<StatRow> conversionBySource() {
        List<StatRow> rows = new ArrayList<>();
        for (Object[] r : leadRepository.conversionBySource()) {
            LeadSource source = (LeadSource) r[0];
            long total = ((Number) r[1]).longValue();
            long admitted = r[2] == null ? 0 : ((Number) r[2]).longValue();
            rows.add(new StatRow(source.getLabel(), total, admitted, percent(admitted, total)));
        }
        rows.sort(Comparator.comparingLong(StatRow::getCount).reversed());
        return rows;
    }

    public List<WorkloadRow> workload() {
        List<WorkloadRow> rows = new ArrayList<>();
        for (Counsellor c : counsellorRepository.findAll(Sort.by("name"))) {
            long open = leadRepository.countOpenByCounsellor(c.getId(), LeadStatus.CLOSED);
            long admitted = leadRepository.countByAssignedToIdAndStatus(c.getId(), LeadStatus.ADMITTED);
            rows.add(new WorkloadRow(c.getId(), c.getName(), c.isActive(), open, admitted));
        }
        return rows;
    }

     public List<StatRow> ageing() {
        Map<AgeingBucket, Long> counts = new EnumMap<>(AgeingBucket.class);
        for (AgeingBucket b : AgeingBucket.values()) counts.put(b, 0L);

        List<Lead> open = leadRepository.findByStatusNotIn(LeadStatus.CLOSED);
        for (Lead lead : open) {
            counts.merge(AgeingBucket.of(lead.getDaysSinceContact()), 1L, Long::sum);
        }
        List<StatRow> rows = new ArrayList<>();
        for (AgeingBucket b : AgeingBucket.values()) {
            rows.add(new StatRow(b.getLabel(), counts.get(b), 0, percent(counts.get(b), open.size())));
        }
        return rows;
    }

     public List<Lead> staleLeads(int days, int limit) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        return leadRepository.findStale(cutoff, LeadStatus.CLOSED).stream().limit(limit).toList();
    }

 
    private Map<LeadStatus, Long> statusCounts() {
        Map<LeadStatus, Long> counts = new EnumMap<>(LeadStatus.class);
        for (LeadStatus s : LeadStatus.values()) counts.put(s, 0L);
        for (Object[] r : leadRepository.countGroupedByStatus()) {
            counts.put((LeadStatus) r[0], ((Number) r[1]).longValue());
        }
        return counts;
    }

    private static double percent(long part, long whole) {
        return whole == 0 ? 0 : Math.round(part * 1000.0 / whole) / 10.0;
    }
}