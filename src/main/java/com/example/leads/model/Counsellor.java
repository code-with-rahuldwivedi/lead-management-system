package com.example.leads.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "counsellors")
public class Counsellor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    @Email
    @Column(length = 150)
    private String email;

    /** Inactive (छुट्टी/resign) counsellor को नया lead कभी नहीं मिलता। */
    @Column(nullable = false)
    private boolean active = true;

    protected Counsellor() { }

    public Counsellor(String name, String email, boolean active) {
        this.name = name;
        this.email = email;
        this.active = active;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Counsellor c && id != null && id.equals(c.id));
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}