package com.telemedicina.prescription.dto.response;

import com.telemedicina.prescription.model.ReferralPriority;
import com.telemedicina.prescription.model.ReferralType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ReferralResponse {
    private Long id;
    private Long consultationId;
    private Long patientId;
    private Long doctorId;
    private ReferralType referralType;
    private ReferralPriority priority;
    private String destination;
    private String reason;
    private Instant issuedAt;
}