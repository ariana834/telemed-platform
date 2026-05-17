package com.telemedicina.appointment.repository;

import com.telemedicina.appointment.dto.response.AvailableSlotResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class AppointmentAvailabilityRepository {

    private final JdbcTemplate jdbc;

    public AppointmentAvailabilityRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Returns all available slots for a given date and appointment duration.
     * Iterates in 10-minute increments through each doctor's schedule,
     * skipping slots that overlap with existing appointments.
     */
    public List<AvailableSlotResponse> findAvailableSlots(LocalDate date, int durationMinutes) {
        int dayOfWeek = date.getDayOfWeek().getValue() - 1; // 0=Mon … 6=Sun

        // Fetch all active doctors with schedules for this day
        List<Map<String, Object>> doctorSchedules = jdbc.queryForList("""
                SELECT d.id        AS doctor_id,
                       u.email     AS email,
                       d.first_name || ' ' || d.last_name AS doctor_name,
                       d.specialization                    AS specialty,
                       ds.start_time,
                       ds.end_time
                FROM doctors d
                JOIN users            u  ON u.id  = d.user_id
                JOIN doctor_schedules ds ON ds.doctor_id = d.id
                WHERE d.is_available = TRUE
                  AND ds.day_of_week = ?
                  AND ds.is_active   = TRUE
                ORDER BY d.id, ds.start_time
                """, dayOfWeek);

        List<AvailableSlotResponse> slots = new ArrayList<>();

        for (Map<String, Object> row : doctorSchedules) {
            Long   doctorId   = ((Number) row.get("doctor_id")).longValue();
            String doctorName = (String) row.get("doctor_name");
            String specialty  = (String) row.get("specialty");

            LocalTime scheduleStart = ((java.sql.Time) row.get("start_time")).toLocalTime();
            LocalTime scheduleEnd   = ((java.sql.Time) row.get("end_time")).toLocalTime();

            LocalDateTime cursor    = LocalDateTime.of(date, scheduleStart);
            LocalDateTime dayEnd    = LocalDateTime.of(date, scheduleEnd);
            LocalDateTime now       = LocalDateTime.now().plusHours(1); // min 1h in advance

            while (!cursor.plusMinutes(durationMinutes).isAfter(dayEnd)) {
                LocalDateTime slotEnd = cursor.plusMinutes(durationMinutes);

                // Only show future slots
                if (cursor.isAfter(now)) {
                    boolean hasOverlap = checkOverlap(doctorId, cursor, slotEnd);
                    if (!hasOverlap) {
                        slots.add(new AvailableSlotResponse(
                                doctorId, doctorName, specialty,
                                cursor, slotEnd, durationMinutes));
                    }
                }
                cursor = cursor.plusMinutes(10);
            }
        }

        return slots;
    }

    private boolean checkOverlap(Long doctorId, LocalDateTime slotStart, LocalDateTime slotEnd) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM appointments
                WHERE doctor_id = ?
                  AND status NOT IN ('CANCELLED', 'NO_SHOW')
                  AND tstzrange(start_time, end_time) &&
                      tstzrange(?::timestamptz, ?::timestamptz)
                """,
                Integer.class,
                doctorId,
                slotStart.toString(),
                slotEnd.toString());
        return count != null && count > 0;
    }

    /**
     * Reschedule an appointment: update start_time, end_time, doctor_id.
     * Validates no overlap before updating.
     */
    public void rescheduleAppointment(Long appointmentId, Long doctorId,
                                      LocalDateTime newStart, int durationMinutes) {
        LocalDateTime newEnd = newStart.plusMinutes(durationMinutes);

        // Check the new slot is free (excluding this appointment)
        Integer overlap = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM appointments
                WHERE doctor_id = ?
                  AND id != ?
                  AND status NOT IN ('CANCELLED', 'NO_SHOW')
                  AND tstzrange(start_time, end_time) &&
                      tstzrange(?::timestamptz, ?::timestamptz)
                """,
                Integer.class,
                doctorId, appointmentId,
                newStart.toString(), newEnd.toString());

        if (overlap != null && overlap > 0) {
            throw new IllegalStateException("SLOT_TAKEN: This slot is no longer available. Please choose another.");
        }

        jdbc.update("""
                UPDATE appointments
                SET doctor_id  = ?,
                    start_time = ?::timestamptz,
                    end_time   = ?::timestamptz,
                    updated_at = NOW()
                WHERE id = ?
                  AND status = 'SCHEDULED'
                """,
                doctorId, newStart.toString(), newEnd.toString(), appointmentId);
    }

    /**
     * Fetch a single appointment with doctor info (for permission checks).
     */
    public Map<String, Object> findByIdWithPatient(Long appointmentId) {
        return jdbc.queryForMap("""
                SELECT a.*, p.user_id AS patient_user_id
                FROM appointments a
                JOIN patients p ON p.id = a.patient_id
                WHERE a.id = ?
                """, appointmentId);
    }
}