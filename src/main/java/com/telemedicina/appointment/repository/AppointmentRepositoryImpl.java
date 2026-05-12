package com.telemedicina.appointment.repository;

import com.telemedicina.appointment.model.Appointment;
import com.telemedicina.appointment.model.AppointmentStatus;
import com.telemedicina.appointment.model.Doctor;
import com.telemedicina.appointment.model.DoctorSchedule;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AppointmentRepositoryImpl implements AppointmentRepository {

    private final JdbcTemplate jdbc;

    @Override
    public Optional<Appointment> findById(Long id) {
        List<Appointment> result = jdbc.query(
                "SELECT * FROM appointments WHERE id = ?",
                appointmentRowMapper(), id
        );
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    @Override
    public List<Appointment> findAllByPatientId(Long patientId) {
        return jdbc.query(
                "SELECT * FROM appointments WHERE patient_id = ? ORDER BY start_time DESC",
                appointmentRowMapper(), patientId
        );
    }

    @Override
    public List<Appointment> findAllByDoctorId(Long doctorId) {
        return jdbc.query(
                "SELECT * FROM appointments WHERE doctor_id = ? ORDER BY start_time DESC",
                appointmentRowMapper(), doctorId
        );
    }

    @Override
    public void updateStatus(Long id, String status) {
        // triggerul trg_sync_consultation_status actualizeaza automat si consultatia
        jdbc.update(
                "UPDATE appointments SET status = ?::appointment_status WHERE id = ?",
                status, id
        );
    }

    @Override
    public void updateNotes(Long id, String notes) {
        jdbc.update("UPDATE appointments SET notes = ? WHERE id = ?", notes, id);
    }

    @Override
    public Optional<Doctor> findDoctorById(Long doctorId) {
        List<Doctor> result = jdbc.query(
                "SELECT * FROM doctors WHERE id = ?",
                doctorRowMapper(), doctorId
        );
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    @Override
    public Optional<Doctor> findDoctorByUserId(Long userId) {
        List<Doctor> result = jdbc.query(
                "SELECT * FROM doctors WHERE user_id = ?",
                doctorRowMapper(), userId
        );
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    @Override
    public List<Doctor> findAllDoctors() {
        return jdbc.query(
                "SELECT * FROM doctors WHERE is_available = TRUE ORDER BY last_name",
                doctorRowMapper()
        );
    }

    @Override
    public List<DoctorSchedule> findScheduleByDoctorId(Long doctorId) {
        return jdbc.query(
                "SELECT * FROM doctor_schedules WHERE doctor_id = ? AND is_active = TRUE ORDER BY day_of_week",
                scheduleRowMapper(), doctorId
        );
    }

    private RowMapper<Appointment> appointmentRowMapper() {
        return (rs, rowNum) -> Appointment.builder()
                .id(rs.getLong("id"))
                .consultationId(rs.getLong("consultation_id"))
                .doctorId(rs.getLong("doctor_id"))
                .patientId(rs.getLong("patient_id"))
                .startTime(rs.getTimestamp("start_time") != null
                        ? rs.getTimestamp("start_time").toInstant() : null)
                .endTime(rs.getTimestamp("end_time") != null
                        ? rs.getTimestamp("end_time").toInstant() : null)
                .durationMinutes(rs.getInt("duration_minutes"))
                .status(AppointmentStatus.valueOf(rs.getString("status")))
                .notes(rs.getString("notes"))
                .createdAt(rs.getTimestamp("created_at") != null
                        ? rs.getTimestamp("created_at").toInstant() : null)
                .updatedAt(rs.getTimestamp("updated_at") != null
                        ? rs.getTimestamp("updated_at").toInstant() : null)
                .build();
    }

    private RowMapper<Doctor> doctorRowMapper() {
        return (rs, rowNum) -> Doctor.builder()
                .id(rs.getLong("id"))
                .userId(rs.getLong("user_id"))
                .firstName(rs.getString("first_name"))
                .lastName(rs.getString("last_name"))
                .specialization(rs.getString("specialization"))
                .licenseNumber(rs.getString("license_number"))
                .phone(rs.getString("phone"))
                .bio(rs.getString("bio"))
                .isAvailable(rs.getBoolean("is_available"))
                .createdAt(rs.getTimestamp("created_at") != null
                        ? rs.getTimestamp("created_at").toInstant() : null)
                .build();
    }

    private RowMapper<DoctorSchedule> scheduleRowMapper() {
        return (rs, rowNum) -> DoctorSchedule.builder()
                .id(rs.getLong("id"))
                .doctorId(rs.getLong("doctor_id"))
                .dayOfWeek(rs.getInt("day_of_week"))
                .startTime(rs.getTime("start_time") != null
                        ? rs.getTime("start_time").toLocalTime() : null)
                .endTime(rs.getTime("end_time") != null
                        ? rs.getTime("end_time").toLocalTime() : null)
                .isActive(rs.getBoolean("is_active"))
                .build();
    }
}