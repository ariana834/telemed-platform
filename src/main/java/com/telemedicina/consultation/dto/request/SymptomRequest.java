package com.telemedicina.consultation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SymptomRequest {

    @NotBlank(message = "Numele simptomului este obligatoriu")
    @Size(max = 255, message = "Simptomul nu poate depasi 255 de caractere")
    private String symptomName;

    // optional - daca nu e specificat, DB-ul defaulteaza la MODERATE
    @Pattern(regexp = "MILD|MODERATE|SEVERE",
            message = "Severitatea trebuie sa fie MILD, MODERATE sau SEVERE")
    private String severity = "MODERATE";
}