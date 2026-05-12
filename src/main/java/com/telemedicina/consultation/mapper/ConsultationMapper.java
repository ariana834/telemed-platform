package com.telemedicina.consultation.mapper;

import com.telemedicina.consultation.dto.response.*;
import com.telemedicina.consultation.model.*;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ConsultationMapper {

    // mapare compacta - pentru liste si raspunsuri la operatii simple
    public ConsultationResponse toResponse(Consultation c) {
        return ConsultationResponse.builder()
                .id(c.getId())
                .patientId(c.getPatientId())
                .status(c.getStatus())
                .complexityLevel(c.getComplexityLevel())
                .emergencyRedirect(c.getEmergencyRedirect())
                .notes(c.getNotes())
                .createdAt(c.getCreatedAt())
                .build();
    }

    // mapare completa - returnat la GET /{id} si dupa calculul diagnosticului
    public ConsultationDetailResponse toDetailResponse(
            Consultation c,
            List<ConsultationSymptom> symptoms,
            List<MedicalFormQuestion> questions,
            List<Diagnosis> diagnoses) {

        return ConsultationDetailResponse.builder()
                .id(c.getId())
                .patientId(c.getPatientId())
                .status(c.getStatus())
                .complexityLevel(c.getComplexityLevel())
                .emergencyRedirect(c.getEmergencyRedirect())
                .notes(c.getNotes())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .symptoms(symptoms)
                .formQuestions(questions.stream()
                        .map(this::toFormResponse)
                        .collect(Collectors.toList()))
                .diagnoses(diagnoses.stream()
                        .map(this::toDiagnosisResponse)
                        .collect(Collectors.toList()))
                .build();
    }

    public MedicalFormResponse toFormResponse(MedicalFormQuestion q) {
        return MedicalFormResponse.builder()
                .id(q.getId())
                .questionText(q.getQuestionText())
                .questionType(q.getQuestionType())
                .options(q.getOptions() != null ? q.getOptions() : Collections.emptyList())
                .orderIndex(q.getOrderIndex())
                .isRequired(q.getIsRequired())
                .build();
    }

    public DiagnosisResponse toDiagnosisResponse(Diagnosis d) {
        return DiagnosisResponse.builder()
                .id(d.getId())
                .diagnosisName(d.getDiagnosisName())
                .diagnosisType(d.getDiagnosisType())
                .complexityLevel(d.getComplexityLevel())
                .icdCode(d.getIcdCode())
                .confidenceScore(d.getConfidenceScore())
                .notes(d.getNotes())
                .build();
    }
}