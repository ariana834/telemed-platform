package com.telemedicina.appointment.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public record AvailableSlotResponse(
        Long doctorId,
        String doctorName,
        String specialty,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime slotStart,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime slotEnd,
        int durationMinutes
) {}