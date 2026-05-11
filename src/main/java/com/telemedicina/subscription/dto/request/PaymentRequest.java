package com.telemedicina.subscription.dto.request;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentRequest {

    @Pattern(regexp = "CARD|TRANSFER|CASH",
            message = "Metoda invalida. Valori acceptate: CARD, TRANSFER, CASH")
    private String paymentMethod = "CARD";

    // transaction_id optional — in productie vine de la procesatorul de plati
    private String transactionId;
}