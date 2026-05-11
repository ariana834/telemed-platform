package com.telemedicina.subscription.dto.response;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
public class SubscriptionResponse {
    private Long id;
    private Long patientId;
    private String type;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private BigDecimal price;
    private OffsetDateTime createdAt;
}