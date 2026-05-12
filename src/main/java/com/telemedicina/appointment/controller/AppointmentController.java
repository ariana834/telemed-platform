package com.telemedicina.appointment.controller;

import com.telemedicina.appointment.dto.request.UpdateAppointmentNotesRequest;
import com.telemedicina.appointment.dto.response.AppointmentResponse;
import com.telemedicina.appointment.dto.response.DoctorResponse;
import com.telemedicina.appointment.service.AppointmentService;
import com.telemedicina.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AppointmentController {
    private final AppointmentService service;

    // toate programarile pacientului curent
    @GetMapping("/api/v1/appointments/my")
    public ResponseEntity<List<AppointmentResponse>> getMyAppointments(
            @AuthenticationPrincipal CustomUserDetails user) {

        return ResponseEntity.ok(service.getMyAppointments(user.getUserId()));
    }

    // detalii programare - accesibil si de pacient si de doctor
    @GetMapping("/api/v1/appointments/{id}")
    public ResponseEntity<AppointmentResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user) {

        return ResponseEntity.ok(service.getById(id, user.getUserId()));
    }

    // pacientul sau doctorul poate anula
    @PostMapping("/api/v1/appointments/{id}/cancel")
    public ResponseEntity<AppointmentResponse> cancel(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user) {

        return ResponseEntity.ok(service.cancelAppointment(id, user.getUserId()));
    }

    // programarile doctorului curent
    @GetMapping("/api/v1/appointments/doctor/my")
    public ResponseEntity<List<AppointmentResponse>> getMyAppointmentsAsDoctor(
            @AuthenticationPrincipal CustomUserDetails user) {

        return ResponseEntity.ok(service.getMyAppointmentsAsDoctor(user.getUserId()));
    }

    // doctorul incepe consultatia
    @PostMapping("/api/v1/appointments/{id}/start")
    public ResponseEntity<AppointmentResponse> start(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user) {

        return ResponseEntity.ok(service.startAppointment(id, user.getUserId()));
    }

    // doctorul finalizeaza consultatia
    @PostMapping("/api/v1/appointments/{id}/complete")
    public ResponseEntity<AppointmentResponse> complete(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user) {

        return ResponseEntity.ok(service.completeAppointment(id, user.getUserId()));
    }

    // doctorul marcheaza pacientul ca neprezent
    @PostMapping("/api/v1/appointments/{id}/no-show")
    public ResponseEntity<AppointmentResponse> noShow(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user) {

        return ResponseEntity.ok(service.markNoShow(id, user.getUserId()));
    }

    // doctorul adauga note la programare (observatii, recomandari)
    @PatchMapping("/api/v1/appointments/{id}/notes")
    public ResponseEntity<AppointmentResponse> updateNotes(
            @PathVariable Long id,
            @RequestBody UpdateAppointmentNotesRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {

        return ResponseEntity.ok(service.updateNotes(id, user.getUserId(), request));
    }

    // lista doctori disponibili cu programul lor
    @GetMapping("/api/v1/doctors")
    public ResponseEntity<List<DoctorResponse>> getAllDoctors() {
        return ResponseEntity.ok(service.getAllDoctors());
    }

    // detalii doctor specific
    @GetMapping("/api/v1/doctors/{id}")
    public ResponseEntity<DoctorResponse> getDoctorById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getDoctorById(id));
    }
}