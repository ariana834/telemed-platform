package com.telemedicina.subscription.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubscriptionRequest {

    @NotBlank(message = "Tipul abonamentului este obligatoriu")
    @Pattern(regexp = "MONTHLY|ANNUAL", message = "Tip invalid. Valori acceptate: MONTHLY, ANNUAL")
    private String type;

    // pretul e optional — triggerul il seteaza automat daca nu e trimis
    // MONTHLY = 50 RON, ANNUAL = 500 RON
}