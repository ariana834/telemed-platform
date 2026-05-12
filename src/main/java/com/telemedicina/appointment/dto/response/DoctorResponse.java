package com.telemedicina.appointment.dto.response;

import com.telemedicina.appointment.model.DoctorSchedule;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DoctorResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String specialization;
    private String phone;
    private String bio;
    private Boolean isAvailable;

    // programul saptamanal
    private List<DoctorSchedule> schedule;
}