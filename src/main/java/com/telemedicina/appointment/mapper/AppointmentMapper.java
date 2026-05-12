package com.telemedicina.appointment.mapper;

import com.telemedicina.appointment.dto.response.AppointmentResponse;
import com.telemedicina.appointment.dto.response.DoctorResponse;
import com.telemedicina.appointment.model.Appointment;
import com.telemedicina.appointment.model.Doctor;
import com.telemedicina.appointment.model.DoctorSchedule;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AppointmentMapper {

    // appointment + doctor info combinate intr-un singur response
    public AppointmentResponse toResponse(Appointment a, Doctor doctor) {
        return AppointmentResponse.builder()
                .id(a.getId())
                .consultationId(a.getConsultationId())
                .patientId(a.getPatientId())
                .status(a.getStatus())
                .startTime(a.getStartTime())
                .endTime(a.getEndTime())
                .durationMinutes(a.getDurationMinutes())
                .notes(a.getNotes())
                .createdAt(a.getCreatedAt())
                .doctorId(doctor.getId())
                .doctorFirstName(doctor.getFirstName())
                .doctorLastName(doctor.getLastName())
                .doctorSpecialization(doctor.getSpecialization())
                .build();
    }

    public DoctorResponse toDoctorResponse(Doctor d, List<DoctorSchedule> schedule) {
        return DoctorResponse.builder()
                .id(d.getId())
                .firstName(d.getFirstName())
                .lastName(d.getLastName())
                .specialization(d.getSpecialization())
                .phone(d.getPhone())
                .bio(d.getBio())
                .isAvailable(d.getIsAvailable())
                .schedule(schedule)
                .build();
    }
}