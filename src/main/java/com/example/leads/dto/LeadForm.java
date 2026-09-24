package com.example.leads.dto;

import com.example.leads.model.LeadSource;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class LeadForm {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name is too long")
    private String name;

    // Phone ka format check service mein normalize karne ke BAAD hoga
    @NotBlank(message = "Phone is required")
    private String phone;

    @Email(message = "Invalid email address")
    @Size(max = 150)
    private String email;

    @NotNull(message = "Select a lead source")
    private LeadSource source;

    private Long courseId;

    @Size(max = 1000, message = "Notes are too long")
    private String notes;

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

	public Long getCourseId() {
		return courseId;
	}

	public void setCourseId(Long courseId) {
		this.courseId = courseId;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}
}