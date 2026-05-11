package com.telemedicina.patient.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Tutorele unui pacient copil.
 * Triggerul trg_validate_guardian din DB blochează inserarea
 * dacă patient-ul nu are age_category = 'CHILD'.
 * Excepția GUARDIAN_ONLY_FOR_CHILD e prinsă în GlobalExceptionHandler → 400.
 */
@Getter
@Setter
@NoArgsConstructor
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

}