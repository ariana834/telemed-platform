package com.telemedicina.appointment.dto.response;

import com.telemedicina.appointment.model.AppointmentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class AppointmentResponse {
    private Long id;
    private String patientFirstName;
    private String patientLastName;
    private Long consultationId;
    private Long patientId;
    private AppointmentStatus status;
    private Instant startTime;
    private Instant endTime;
    private Integer durationMinutes;
    private String notes;
    private Instant createdAt;

    // info doctor inclusa direct
    private Long doctorId;
    private String doctorFirstName;
    private String doctorLastName;
    private String doctorSpecialization;
}