package com.telemedicina.patient.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO pentru adăugarea unui tutore la un pacient copil.
 * patientId (ID-ul copilului) vine din path variable, nu din body.
 * guardianUserId vine din JWT — userul curent devine tutorele.
 */
public class GuardianRequest {

    @NotBlank(message = "Prenumele tutorelui este obligatoriu")
    @Size(max = 100)
    private String firstName;

    @NotBlank(message = "Numele tutorelui este obligatoriu")
    @Size(max = 100)
    private String lastName;

    private String phone;

    @Email(message = "Email invalid")
    private String email;

    // PARENT, LEGAL_GUARDIAN, GRANDPARENT etc.
    private String relationship = "PARENT";

    public GuardianRequest() {}

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
}