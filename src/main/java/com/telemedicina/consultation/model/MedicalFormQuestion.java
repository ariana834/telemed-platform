package com.telemedicina.consultation.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalFormQuestion {
    private Long id;
    private Long consultationId;
    private String questionText;
    private QuestionType questionType;

    // vine ca JSONB din postgres: ["Optiune1", "Optiune2", ...], e populat doar pentru MULTIPLE_CHOICE si CHECKBOX, null pentru restul
    private List<String> options;

    private Integer orderIndex;
    private Boolean isRequired;
    private Instant createdAt;
}