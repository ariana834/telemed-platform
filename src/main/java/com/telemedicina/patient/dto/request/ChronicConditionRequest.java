package com.telemedicina.patient.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * DTO pentru adăugarea unei afecțiuni cronice.
 * Aceste date sunt critice pentru generate_medical_form() —
 * funcția PL/pgSQL le citește direct din chronic_conditions
 * ca să adapteze întrebările generate.
 */
public class ChronicConditionRequest {

    @NotBlank(message = "Numele afecțiunii este obligatoriu")
    @Size(max = 255)
    private String conditionName;

    private LocalDate diagnosedDate;

    @Pattern(regexp = "MILD|MODERATE|SEVERE",
            message = "Severitate invalidă. Valori acceptate: MILD, MODERATE, SEVERE")
    private String severity = "MODERATE";

    private String notes;

    public ChronicConditionRequest() {}

    public String getConditionName() { return conditionName; }
    public void setConditionName(String conditionName) { this.conditionName = conditionName; }

    public LocalDate getDiagnosedDate() { return diagnosedDate; }
    public void setDiagnosedDate(LocalDate diagnosedDate) { this.diagnosedDate = diagnosedDate; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}