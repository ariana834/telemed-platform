package com.telemedicina.patient.model;

import java.time.OffsetDateTime;

/**
 * Tutorele unui pacient copil.
 * Triggerul trg_validate_guardian din DB blochează inserarea
 * dacă patient-ul nu are age_category = 'CHILD'.
 * Excepția GUARDIAN_ONLY_FOR_CHILD e prinsă în GlobalExceptionHandler → 400.
 */
public class Guardian {
    private Long id;
    private Long patientId;         // ID-ul pacientului copil
    private Long guardianUserId;    // ID-ul userului părinte/tutore
    private String firstName;
    private String lastName;
    private String phone;
    private String email;
    private String relationship;    // PARENT, LEGAL_GUARDIAN etc.
    private OffsetDateTime createdAt;

    public Guardian() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }

    public Long getGuardianUserId() { return guardianUserId; }
    public void setGuardianUserId(Long guardianUserId) { this.guardianUserId = guardianUserId; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRelationship() { return relationship; }
    public void setRelationship(String relationship) { this.relationship = relationship; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}