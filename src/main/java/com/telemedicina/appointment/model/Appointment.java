package com.telemedicina.appointment.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Appointment {
    private Long id;
    private Long consultationId;
    private Long doctorId;
    private Long patientId;
    private Instant startTime;
    private Instant endTime;
    private Integer durationMinutes;  //de ales intre 10,20,30
    private AppointmentStatus status;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
}