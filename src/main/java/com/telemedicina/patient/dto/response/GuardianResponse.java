package com.telemedicina.patient.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
public class GuardianResponse {
    private Long id;
    private Long patientId;
    private Long guardianUserId;
    private String firstName;
    private String lastName;
    private String phone;
    private String email;
    private String relationship;
    private OffsetDateTime createdAt;
}