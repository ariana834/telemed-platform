package com.telemedicina.consultation.dto.response;

import com.telemedicina.consultation.model.ComplexityLevel;
import com.telemedicina.consultation.model.ConsultationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ConsultationResponse {
    private Long id;
    private Long patientId;
    private ConsultationStatus status;
    private ComplexityLevel complexityLevel;
    private Boolean emergencyRedirect;
    private String notes;
    private Instant createdAt;
    private Integer symptomCount;
}