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
public class ConsultationSymptom {
    private Long id;
    private Long consultationId;
    private String symptomName;
    private String severity;
    private Integer orderIndex;
    private Instant createdAt;
}