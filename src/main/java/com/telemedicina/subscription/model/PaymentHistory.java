package com.telemedicina.subscription.model;

import lombok.Data;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
public class PaymentHistory {
    private Long id;
    private Long subscriptionId;
    private BigDecimal amount;
    private OffsetDateTime paymentDate;
    private String paymentMethod; // CARD / TRANSFER / CASH
    private String status;        // PENDING / COMPLETED / FAILED / REFUNDED
    private String transactionId;
    private String notes;
}