package com.telemedicina.appointment.service;

import com.telemedicina.appointment.dto.request.UpdateAppointmentNotesRequest;
import com.telemedicina.appointment.dto.response.AppointmentResponse;
import com.telemedicina.appointment.dto.response.DoctorResponse;

import java.util.List;

public interface AppointmentService {

    //pacient
    AppointmentResponse getById(Long id, Long userId);
    List<AppointmentResponse> getMyAppointments(Long userId);

    //doctor
    List<AppointmentResponse> getMyAppointmentsAsDoctor(Long userId);
    AppointmentResponse startAppointment(Long id, Long userId);
    AppointmentResponse completeAppointment(Long id, Long userId);
    AppointmentResponse cancelAppointment(Long id, Long userId);
    AppointmentResponse markNoShow(Long id, Long userId);
    AppointmentResponse updateNotes(Long id, Long userId, UpdateAppointmentNotesRequest request);

    //doctori care e vizibil pentru toti
    List<DoctorResponse> getAllDoctors();
    DoctorResponse getDoctorById(Long doctorId);
}