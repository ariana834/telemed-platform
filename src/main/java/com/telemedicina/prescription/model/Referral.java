package com.telemedicina.prescription.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Referral {
    private Long id;
    private Long consultationId;
    private Long patientId;
    private Long doctorId;
    private ReferralType referralType;
    private ReferralPriority priority;
    private String doctorName;
    private String destination;
    private String reason;
    private Instant issuedAt;
}