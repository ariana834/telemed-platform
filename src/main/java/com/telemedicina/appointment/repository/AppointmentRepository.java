package com.telemedicina.appointment.repository;

import com.telemedicina.appointment.model.Appointment;
import com.telemedicina.appointment.model.Doctor;
import com.telemedicina.appointment.model.DoctorSchedule;

import java.util.List;
import java.util.Optional;

public interface AppointmentRepository {

    //doctori
    Optional<Appointment> findById(Long id);
    List<Appointment> findAllByPatientId(Long patientId);
    List<Appointment> findAllByDoctorId(Long doctorId);
    void updateStatus(Long id, String status);
    void updateNotes(Long id, String notes);
    Optional<String[]> findPatientName(Long patientId);

    //doctori
    Optional<Doctor> findDoctorById(Long doctorId);
    Optional<Doctor> findDoctorByUserId(Long userId);
    List<Doctor> findAllDoctors();
    List<DoctorSchedule> findScheduleByDoctorId(Long doctorId);
}