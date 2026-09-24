package com.example.leads.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "leads", indexes = {
        @Index(name = "idx_lead_status", columnList = "status"),
        @Index(name = "idx_lead_assigned", columnList = "assigned_to_id"),
        @Index(name = "idx_lead_source", columnList = "source"),
        @Index(name = "idx_lead_created", columnList = "created_at")
})
public class Lead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @NotBlank(message = "Name is required")
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit Indian mobile number")
    @Column(nullable = false, unique = true, length = 10)
    private String phone;

    @Email(message = "Invalid email address")
    @Size(max = 150)
    @Column(length = 150)
    private String email;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LeadSource source;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_interest_id")
    private Course courseInterest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private Counsellor assignedTo;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LeadStatus status = LeadStatus.NEW;

    @Size(max = 255)
    private String lostReason;

    @Size(max = 1000)
    @Column(length = 1000)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_contacted_at")
    private LocalDateTime lastContactedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;    
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public long getDaysSinceContact() {
        LocalDateTime reference = lastContactedAt != null ? lastContactedAt : createdAt;
        if (reference == null) return 0;
        return Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(reference, LocalDateTime.now()));
    }

    
    public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public LeadSource getSource() {
		return source;
	}

	public void setSource(LeadSource source) {
		this.source = source;
	}

	public Course getCourseInterest() {
		return courseInterest;
	}

	public void setCourseInterest(Course courseInterest) {
		this.courseInterest = courseInterest;
	}

	public Counsellor getAssignedTo() {
		return assignedTo;
	}

	public void setAssignedTo(Counsellor assignedTo) {
		this.assignedTo = assignedTo;
	}

	public LeadStatus getStatus() {
		return status;
	}

	public void setStatus(LeadStatus status) {
		this.status = status;
	}

	public String getLostReason() {
		return lostReason;
	}

	public void setLostReason(String lostReason) {
		this.lostReason = lostReason;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

	public LocalDateTime getLastContactedAt() {
		return lastContactedAt;
	}

	public void setLastContactedAt(LocalDateTime lastContactedAt) {
		this.lastContactedAt = lastContactedAt;
	}

	@Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Lead l && id != null && id.equals(l.id));
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}