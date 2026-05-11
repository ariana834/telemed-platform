package com.telemedicina.subscription.model;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
public class Subscription {
    private Long id;
    private Long patientId;
    private String type;        // MONTHLY / ANNUAL
    private LocalDate startDate;
    private LocalDate endDate;  // calculat automat de trigger
    private String status;      // PENDING / ACTIVE / EXPIRED / CANCELLED
    private BigDecimal price;   // setat automat de trigger: 50 RON lunar, 500 RON anual
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}