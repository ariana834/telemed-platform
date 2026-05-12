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
public class Diagnosis {
    private Long id;
    private Long consultationId;
    private String diagnosisName;
    private DiagnosisType diagnosisType;
    private ComplexityLevel complexityLevel;
    private String icdCode;          // cod ICD-10 international (ex: J10 pentru gripa)
    private Integer confidenceScore; // 0-100, niciodata 100 pentru auto-generate (max 90)
    private String notes;
    private Long createdBy;          // null daca e generat automat, altfel punem id ul doctorului
    private Instant createdAt;
}