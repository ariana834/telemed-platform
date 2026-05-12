package com.telemedicina.prescription.dto.request;

import com.telemedicina.prescription.model.ReferralPriority;
import com.telemedicina.prescription.model.ReferralType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateReferralRequest {
    @NotNull(message = "ID-ul consultatiei este obligatoriu")
    private Long consultationId;

    @NotNull(message = "Tipul trimiterii este obligatoriu (HOSPITAL sau INVESTIGATION)")
    private ReferralType referralType;

    @NotNull(message = "Prioritatea este obligatorie (ROUTINE, URGENT, EMERGENCY)")
    private ReferralPriority priority;

    @NotBlank(message = "Destinatia este obligatorie")
    private String destination;

    @NotBlank(message = "Motivul trimiterii este obligatoriu")
    private String reason;
}