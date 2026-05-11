package com.telemedicina.patient.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Afecțiune cronică a unui pacient.
 * Esențială pentru generate_medical_form() — funcția PL/pgSQL
 * verifică afecțiunile active ale pacientului și adaptează
 * întrebările fișei medicale în consecință.
 *
 * Constrângerea uq_active_condition din DB garantează că același
 * diagnostic activ nu apare de două ori (UNIQUE pe patient_id + condition_name + is_active).
 */
@Getter
@Setter
@NoArgsConstructor
public class ChronicCondition {
    private Long id;
    private Long patientId;
    private String conditionName;
    private LocalDate diagnosedDate;
    private String severity;        // MILD, MODERATE, SEVERE
    private boolean isActive;
    private String notes;
    private OffsetDateTime createdAt;
}