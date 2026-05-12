package com.telemedicina.consultation.dto.response;

import com.telemedicina.consultation.model.ComplexityLevel;
import com.telemedicina.consultation.model.ConsultationStatus;
import com.telemedicina.consultation.model.ConsultationSymptom;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

// raspuns complet - returnat la GET /{id} si dupa submitAnswers (cand avem si diagnostice)
@Data
@Builder
public class ConsultationDetailResponse {
    private Long id;
    private Long patientId;
    private ConsultationStatus status;
    private ComplexityLevel complexityLevel;
    private Boolean emergencyRedirect;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;

    private List<ConsultationSymptom> symptoms;
    private List<MedicalFormResponse> formQuestions;
    private List<DiagnosisResponse> diagnoses;
}