package com.example.leads.repository;

import com.example.leads.model.Lead;
import com.example.leads.model.LeadStatus;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LeadRepository extends JpaRepository<Lead, Long>, JpaSpecificationExecutor<Lead> {

    Optional<Lead> findByPhone(String phone);

    boolean existsByPhone(String phone);

    @EntityGraph(attributePaths = {"assignedTo", "courseInterest"})
    Optional<Lead> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"assignedTo", "courseInterest"})
    List<Lead> findAll(Specification<Lead> spec, Sort sort);

    List<Lead> findByStatusNotIn(Collection<LeadStatus> statuses);

    List<Lead> findByAssignedToIdAndStatusNotIn(Long counsellorId, Collection<LeadStatus> statuses);

    long countByAssignedToIdAndStatus(Long counsellorId, LeadStatus status);

    /** Funnel: [status, count] */
    @Query("select l.status, count(l) from Lead l group by l.status")
    List<Object[]> countGroupedByStatus();

    /** Source-wise conversion: [source, total, admitted] */
    @Query("""
           select l.source, count(l),
                  sum(case when l.status = com.example.leads.model.LeadStatus.ADMITTED then 1 else 0 end)
           from Lead l group by l.source
           """)
    List<Object[]> conversionBySource();

    @Query("select count(l) from Lead l where l.assignedTo.id = :counsellorId and l.status not in :closed")
    long countOpenByCounsellor(@Param("counsellorId") Long counsellorId,
                               @Param("closed") Collection<LeadStatus> closed);

    @Query("""
           select l from Lead l
           left join fetch l.assignedTo
           left join fetch l.courseInterest
           where l.status not in :closed
             and coalesce(l.lastContactedAt, l.createdAt) < :cutoff
           order by coalesce(l.lastContactedAt, l.createdAt) asc
           """)
    List<Lead> findStale(@Param("cutoff") LocalDateTime cutoff,
                         @Param("closed") Collection<LeadStatus> closed);
}