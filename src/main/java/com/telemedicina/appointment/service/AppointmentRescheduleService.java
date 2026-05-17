package com.telemedicina.appointment.service;

import com.telemedicina.appointment.dto.request.RescheduleRequest;
import com.telemedicina.appointment.dto.response.AvailableSlotResponse;
import com.telemedicina.appointment.repository.AppointmentAvailabilityRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class AppointmentRescheduleService {

    private final AppointmentAvailabilityRepository availabilityRepo;

    public AppointmentRescheduleService(AppointmentAvailabilityRepository availabilityRepo) {
        this.availabilityRepo = availabilityRepo;
    }

    public List<AvailableSlotResponse> getAvailableSlots(LocalDate date, int durationMinutes) {
        if (durationMinutes != 10 && durationMinutes != 20 && durationMinutes != 30) {
            throw new IllegalArgumentException("Duration must be 10, 20, or 30 minutes.");
        }
        return availabilityRepo.findAvailableSlots(date, durationMinutes);
    }

    public void reschedule(Long appointmentId, Long currentUserId, RescheduleRequest request) {
        Map<String, Object> appt = availabilityRepo.findByIdWithPatient(appointmentId);

        Long patientUserId = ((Number) appt.get("patient_user_id")).longValue();
        if (!patientUserId.equals(currentUserId)) {
            throw new AccessDeniedException("You can only reschedule your own appointments.");
        }

        String status = (String) appt.get("status");
        if (!"SCHEDULED".equals(status)) {
            throw new IllegalStateException("Only SCHEDULED appointments can be rescheduled.");
        }

        int duration = ((Number) appt.get("duration_minutes")).intValue();

        availabilityRepo.rescheduleAppointment(
                appointmentId,
                request.doctorId(),
                request.newStartTime(),
                duration);
    }
}