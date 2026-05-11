package com.telemedicina.patient.model;

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
public class ChronicCondition {
    private Long id;
    private Long patientId;
    private String conditionName;
    private LocalDate diagnosedDate;
    private String severity;        // MILD, MODERATE, SEVERE
    private boolean isActive;
    private String notes;
    private OffsetDateTime createdAt;

    public ChronicCondition() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }

    public String getConditionName() { return conditionName; }
    public void setConditionName(String conditionName) { this.conditionName = conditionName; }

    public LocalDate getDiagnosedDate() { return diagnosedDate; }
    public void setDiagnosedDate(LocalDate diagnosedDate) { this.diagnosedDate = diagnosedDate; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}