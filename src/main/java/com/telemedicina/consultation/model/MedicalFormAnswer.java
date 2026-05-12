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
public class MedicalFormAnswer {
    private Long id;
    private Long questionId;
    private Long consultationId;
    private String answerText;
    private Instant createdAt;
}