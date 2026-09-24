package com.example.leads.repository;

import com.example.leads.model.Counsellor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CounsellorRepository extends JpaRepository<Counsellor, Long> {
    List<Counsellor> findByActiveTrueOrderByNameAsc();
}