package com.example.leads.repository;

import com.example.leads.model.FollowUp;
import com.example.leads.model.LeadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface FollowUpRepository extends JpaRepository<FollowUp, Long> {

    List<FollowUp> findByLeadIdOrderByDueAtAsc(Long leadId);

    @Query("""
           select f from FollowUp f
           join fetch f.lead l
           left join fetch l.assignedTo
           where f.done = false and f.dueAt < :now and l.status not in :closed
           order by f.dueAt asc
           """)
    List<FollowUp> findOverdue(@Param("now") LocalDateTime now,
                               @Param("closed") Collection<LeadStatus> closed);

    @Query("""
           select count(f) from FollowUp f
           where f.done = false and f.dueAt < :now and f.lead.status not in :closed
           """)
    long countOverdue(@Param("now") LocalDateTime now,
                      @Param("closed") Collection<LeadStatus> closed);
}