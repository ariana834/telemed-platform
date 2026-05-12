package com.telemedicina.appointment.service;

import com.telemedicina.appointment.dto.request.UpdateAppointmentNotesRequest;
import com.telemedicina.appointment.dto.response.AppointmentResponse;
import com.telemedicina.appointment.dto.response.DoctorResponse;
import com.telemedicina.appointment.mapper.AppointmentMapper;
import com.telemedicina.appointment.model.Appointment;
import com.telemedicina.appointment.model.AppointmentStatus;
import com.telemedicina.appointment.model.Doctor;
import com.telemedicina.appointment.repository.AppointmentRepository;
import com.telemedicina.patient.repository.PatientRepository;
import com.telemedicina.shared.exception.ApiException;
import com.telemedicina.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepo;
    private final PatientRepository patientRepo;
    private final AppointmentMapper mapper;

    @Override
    public AppointmentResponse getById(Long id, Long userId) {
        Appointment appointment = getAppointmentById(id);
        verifyPatientAccess(appointment, userId);
        return buildResponse(appointment);
    }

    @Override
    public List<AppointmentResponse> getMyAppointments(Long userId) {
        Long patientId = getPatientId(userId);
        return appointmentRepo.findAllByPatientId(patientId).stream()
                .map(this::buildResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<AppointmentResponse> getMyAppointmentsAsDoctor(Long userId) {
        Doctor doctor = getDoctorByUserId(userId);
        return appointmentRepo.findAllByDoctorId(doctor.getId()).stream()
                .map(this::buildResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AppointmentResponse startAppointment(Long id, Long userId) {
        Appointment appointment = getAndVerifyDoctorAccess(id, userId);

        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new ApiException("Programarea nu poate fi inceputa. Status curent: " + appointment.getStatus(),
                    HttpStatus.BAD_REQUEST);
        }

        // triggerul trg_sync_consultation_status actualizeaza si consultatia automat
        appointmentRepo.updateStatus(id, "IN_PROGRESS");
        return buildResponse(getAppointmentById(id));
    }

    @Override
    @Transactional
    public AppointmentResponse completeAppointment(Long id, Long userId) {
        Appointment appointment = getAndVerifyDoctorAccess(id, userId);

        if (appointment.getStatus() != AppointmentStatus.IN_PROGRESS) {
            throw new ApiException("Programarea nu poate fi finalizata. Status curent: " + appointment.getStatus(),
                    HttpStatus.BAD_REQUEST);
        }

        appointmentRepo.updateStatus(id, "COMPLETED");
        return buildResponse(getAppointmentById(id));
    }

    @Override
    @Transactional
    public AppointmentResponse cancelAppointment(Long id, Long userId) {
        Appointment appointment = getAppointmentById(id);

        // atat pacientul cat si doctorul pot anula
        verifyPatientOrDoctorAccess(appointment, userId);

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new ApiException("Nu poti anula o programare deja finalizata.", HttpStatus.BAD_REQUEST);
        }

        appointmentRepo.updateStatus(id, "CANCELLED");
        return buildResponse(getAppointmentById(id));
    }

    @Override
    @Transactional
    public AppointmentResponse markNoShow(Long id, Long userId) {
        Appointment appointment = getAndVerifyDoctorAccess(id, userId);

        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new ApiException("No-show se poate marca doar pentru programari in asteptare.",
                    HttpStatus.BAD_REQUEST);
        }

        appointmentRepo.updateStatus(id, "NO_SHOW");
        return buildResponse(getAppointmentById(id));
    }

    @Override
    @Transactional
    public AppointmentResponse updateNotes(Long id, Long userId, UpdateAppointmentNotesRequest request) {
        getAndVerifyDoctorAccess(id, userId);
        appointmentRepo.updateNotes(id, request.getNotes());
        return buildResponse(getAppointmentById(id));
    }


    @Override
    public List<DoctorResponse> getAllDoctors() {
        return appointmentRepo.findAllDoctors().stream()
                .map(d -> mapper.toDoctorResponse(d, appointmentRepo.findScheduleByDoctorId(d.getId())))
                .collect(Collectors.toList());
    }

    @Override
    public DoctorResponse getDoctorById(Long doctorId) {
        Doctor doctor = appointmentRepo.findDoctorById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", doctorId));
        return mapper.toDoctorResponse(doctor, appointmentRepo.findScheduleByDoctorId(doctorId));
    }

    private Appointment getAppointmentById(Long id) {
        return appointmentRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Programare", id));
    }

    private Long getPatientId(Long userId) {
        return patientRepo.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Pacient", userId))
                .getId();
    }

    private Doctor getDoctorByUserId(Long userId) {
        return appointmentRepo.findDoctorByUserId(userId)
                .orElseThrow(() -> new ApiException("Nu esti inregistrat ca doctor.", HttpStatus.FORBIDDEN));
    }

    // verifica ca programarea apartine pacientului curent
    private void verifyPatientAccess(Appointment appointment, Long userId) {
        Long patientId = getPatientId(userId);
        if (!appointment.getPatientId().equals(patientId)) {
            throw new ApiException("Nu ai acces la aceasta programare.", HttpStatus.FORBIDDEN);
        }
    }

    // verifica ca doctorul curent e cel asignat la programare
    private Appointment getAndVerifyDoctorAccess(Long appointmentId, Long userId) {
        Appointment appointment = getAppointmentById(appointmentId);
        Doctor doctor = getDoctorByUserId(userId);
        if (!appointment.getDoctorId().equals(doctor.getId())) {
            throw new ApiException("Aceasta programare nu iti este asignata.", HttpStatus.FORBIDDEN);
        }
        return appointment;
    }

    // atat pacientul cat si doctorul pot anula
    private void verifyPatientOrDoctorAccess(Appointment appointment, Long userId) {
        boolean isPatient = patientRepo.findByUserId(userId)
                .map(p -> p.getId().equals(appointment.getPatientId()))
                .orElse(false);

        boolean isDoctor = appointmentRepo.findDoctorByUserId(userId)
                .map(d -> d.getId().equals(appointment.getDoctorId()))
                .orElse(false);

        if (!isPatient && !isDoctor) {
            throw new ApiException("Nu ai acces la aceasta programare.", HttpStatus.FORBIDDEN);
        }
    }

    // construieste response-ul cu doctor info inclus
    private AppointmentResponse buildResponse(Appointment a) {
        Doctor doctor = appointmentRepo.findDoctorById(a.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", a.getDoctorId()));
        return mapper.toResponse(a, doctor);
    }
}