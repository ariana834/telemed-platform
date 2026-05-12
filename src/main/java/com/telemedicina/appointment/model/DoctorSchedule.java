package com.telemedicina.appointment.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorSchedule {
    private Long id;
    private Long doctorId;
    private Integer dayOfWeek;  // 0=Luni, 1=Marti, ..., 6=Duminica
    private LocalTime startTime;
    private LocalTime endTime;
    private Boolean isActive;
}