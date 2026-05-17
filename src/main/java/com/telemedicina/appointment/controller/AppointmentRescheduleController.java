package com.telemedicina.appointment.controller;

import com.telemedicina.appointment.dto.response.AvailableSlotResponse;
import com.telemedicina.appointment.service.AppointmentRescheduleService;
import com.telemedicina.appointment.dto.request.RescheduleRequest;
import com.telemedicina.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/appointments")
public class AppointmentRescheduleController {

    private final AppointmentRescheduleService service;
    private final JwtService jwtService;

    public AppointmentRescheduleController(AppointmentRescheduleService service,
                                           JwtService jwtService) {
        this.service     = service;
        this.jwtService  = jwtService;
    }

    /**
     * GET /api/v1/appointments/available-slots?date=2026-05-20&duration=20
     * Returns free slots for all available doctors on the given date.
     */
    @GetMapping("/available-slots")
    public ResponseEntity<List<AvailableSlotResponse>> getAvailableSlots(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "20") int duration) {

        List<AvailableSlotResponse> slots = service.getAvailableSlots(date, duration);
        return ResponseEntity.ok(slots);
    }

    /**
     * PATCH /api/v1/appointments/{id}/reschedule
     * Body: { doctorId: 1, newStartTime: "2026-05-20T09:30:00" }
     */
    @PatchMapping("/{id}/reschedule")
    public ResponseEntity<?> reschedule(
            @PathVariable Long id,
            @Valid @RequestBody RescheduleRequest request,
            @RequestHeader("Authorization") String authHeader) {

        Long userId = jwtService.extractUserId(authHeader.replace("Bearer ", ""));
        service.reschedule(id, userId, request);
        return ResponseEntity.ok(Map.of("message", "Appointment rescheduled successfully."));
    }
}