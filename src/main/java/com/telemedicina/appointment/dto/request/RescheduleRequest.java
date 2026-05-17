package com.telemedicina.appointment.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record RescheduleRequest(
        @NotNull Long doctorId,
        @NotNull
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime newStartTime
) {}