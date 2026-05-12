package com.telemedicina.consultation.dto.response;

import com.telemedicina.consultation.model.ComplexityLevel;
import com.telemedicina.consultation.model.DiagnosisType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DiagnosisResponse {
    private Long id;
    private String diagnosisName;
    private DiagnosisType diagnosisType;
    private ComplexityLevel complexityLevel;
    private String icdCode;
    private Integer confidenceScore;
    private String notes;
}