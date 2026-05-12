package com.telemedicina.prescription.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreatePrescriptionRequest {

    @NotNull(message = "ID-ul consultatiei este obligatoriu")
    private Long consultationId;

    @NotNull(message = "ID-ul diagnosticului este obligatoriu")
    private Long diagnosisId;

    // cat timp e valabila reteta - in zile (optional, default 30)
    private Integer validDays = 30;

    @NotEmpty(message = "O reteta trebuie sa contina cel putin un medicament")
    @Valid
    private List<MedicationItem> medications;

    @Data
    public static class MedicationItem {

        @NotNull(message = "Numele medicamentului este obligatoriu")
        private String medicationName;

        @NotNull(message = "Dozajul este obligatoriu")
        private String dosage;

        @NotNull(message = "Frecventa este obligatorie")
        private String frequency;

        private Integer durationDays;
        private String instructions;
    }
}