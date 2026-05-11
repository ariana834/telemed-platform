package com.telemedicina.subscription.dto.response;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
public class PaymentResponse {
    private Long id;
    private Long subscriptionId;
    private BigDecimal amount;
    private OffsetDateTime paymentDate;
    private String paymentMethod;
    private String status;
    private String transactionId;
}