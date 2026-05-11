package com.telemedicina.patient.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Setter
@Getter
@NoArgsConstructor
public class ChronicConditionResponse {
    private Long id;
    private Long patientId;
    private String conditionName;
    private LocalDate diagnosedDate;
    private String severity;
    private boolean isActive;
    private String notes;
    private OffsetDateTime createdAt;

}