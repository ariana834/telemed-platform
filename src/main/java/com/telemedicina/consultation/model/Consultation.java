package com.telemedicina.consultation.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Consultation {
    private Long id;
    private Long patientId;
    private ConsultationStatus status;
    private ComplexityLevel complexityLevel;  // setat de generate_medical_form(), poate fi null initial
    private Boolean emergencyRedirect;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
}