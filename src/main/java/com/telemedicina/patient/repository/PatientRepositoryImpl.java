package com.telemedicina.patient.repository;

import com.telemedicina.patient.dto.ChronicConditionRequest;
import com.telemedicina.patient.dto.GuardianRequest;
import com.telemedicina.patient.dto.PatientRequest;
import com.telemedicina.patient.mapper.PatientMapper;
import com.telemedicina.patient.model.ChronicCondition;
import com.telemedicina.patient.model.Guardian;
import com.telemedicina.patient.model.Patient;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

/**
 * Acces la baza de date pentru tot ce ține de pacienți.
 * Nu există ORM — fiecare query e SQL explicit.
 *
 * Câteva note despre casting-ul PostgreSQL:
 * - `?::gender`       — cast explicit la tipul ENUM definit în DB
 * - `age` și `age_category` nu apar în INSERT — sunt calculate de DB
 * - TIMESTAMPTZ se mapează automat la OffsetDateTime prin driverul JDBC
 */
@Repository
public class PatientRepositoryImpl implements PatientRepository {

    private final JdbcTemplate jdbc;
    private final PatientMapper mapper;

    public PatientRepositoryImpl(JdbcTemplate jdbc, PatientMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    // ─── Patient ──────────────────────────────────────────────────────────────

    @Override
    public Long createPatient(Long userId, PatientRequest req) {
        // age și age_category sunt omise intenționat — DB-ul le calculează automat
        // prin coloana GENERATED ALWAYS AS și triggerul trg_calculate_age_category
        String sql = """
                INSERT INTO patients (user_id, first_name, last_name, birth_date,
                                      gender, blood_type, phone, cnp, address)
                VALUES (?, ?, ?, ?, ?::gender, ?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(sql, new String[]{"id"});
            ps.setLong(1, userId);
            ps.setString(2, req.getFirstName());
            ps.setString(3, req.getLastName());
            ps.setDate(4, Date.valueOf(req.getBirthDate()));
            ps.setString(5, req.getGender());
            ps.setString(6, req.getBloodType());    // poate fi null — JDBC gestionează
            ps.setString(7, req.getPhone());
            ps.setString(8, req.getCnp());
            ps.setString(9, req.getAddress());
            return ps;
        }, keyHolder);

        return keyHolder.getKey().longValue();
    }

    @Override
    public Optional<Patient> findByUserId(Long userId) {
        String sql = """
                SELECT id, user_id, first_name, last_name, birth_date,
                       age, age_category::text, gender::text,
                       blood_type, phone, cnp, address,
                       is_guardian, created_at, updated_at
                FROM patients
                WHERE user_id = ?
                """;

        List<Patient> results = jdbc.query(sql, mapper::mapToPatient, userId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public Optional<Patient> findById(Long patientId) {
        String sql = """
                SELECT id, user_id, first_name, last_name, birth_date,
                       age, age_category::text, gender::text,
                       blood_type, phone, cnp, address,
                       is_guardian, created_at, updated_at
                FROM patients
                WHERE id = ?
                """;

        List<Patient> results = jdbc.query(sql, mapper::mapToPatient, patientId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public void updatePatient(Long userId, PatientRequest req) {
        // Nu permitem actualizarea user_id sau a câmpurilor calculate de DB
        String sql = """
                UPDATE patients
                SET first_name  = ?,
                    last_name   = ?,
                    birth_date  = ?,
                    gender      = ?::gender,
                    blood_type  = ?,
                    phone       = ?,
                    cnp         = ?,
                    address     = ?
                WHERE user_id = ?
                """;

        jdbc.update(sql,
                req.getFirstName(),
                req.getLastName(),
                Date.valueOf(req.getBirthDate()),
                req.getGender(),
                req.getBloodType(),
                req.getPhone(),
                req.getCnp(),
                req.getAddress(),
                userId
        );
        // updated_at e setat automat de triggerul trg_patients_updated_at
    }

    @Override
    public boolean existsByUserId(Long userId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM patients WHERE user_id = ?",
                Integer.class, userId
        );
        return count != null && count > 0;
    }

    // ─── Guardian ─────────────────────────────────────────────────────────────

    @Override
    public Long createGuardian(Long patientId, Long guardianUserId, GuardianRequest req) {
        // Triggerul trg_validate_guardian verifică că patientId e un CHILD
        // Dacă nu e, aruncă GUARDIAN_ONLY_FOR_CHILD → prins în GlobalExceptionHandler
        String sql = """
                INSERT INTO guardians (patient_id, guardian_user_id,
                                       first_name, last_name, phone, email, relationship)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(sql, new String[]{"id"});
            ps.setLong(1, patientId);
            ps.setLong(2, guardianUserId);
            ps.setString(3, req.getFirstName());
            ps.setString(4, req.getLastName());
            ps.setString(5, req.getPhone());
            ps.setString(6, req.getEmail());
            ps.setString(7, req.getRelationship());
            return ps;
        }, keyHolder);

        return keyHolder.getKey().longValue();
    }

    @Override
    public Optional<Guardian> findGuardianByPatientId(Long patientId) {
        String sql = """
                SELECT id, patient_id, guardian_user_id,
                       first_name, last_name, phone, email, relationship, created_at
                FROM guardians
                WHERE patient_id = ?
                """;

        List<Guardian> results = jdbc.query(sql, mapper::mapToGuardian, patientId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    // ─── Chronic Conditions ───────────────────────────────────────────────────

    @Override
    public Long createChronicCondition(Long patientId, ChronicConditionRequest req) {
        // Constrângerea uq_active_condition din DB previne duplicate active
        String sql = """
                INSERT INTO chronic_conditions (patient_id, condition_name,
                                                diagnosed_date, severity, notes)
                VALUES (?, ?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(sql, new String[]{"id"});
            ps.setLong(1, patientId);
            ps.setString(2, req.getConditionName());
            ps.setDate(3, req.getDiagnosedDate() != null
                    ? Date.valueOf(req.getDiagnosedDate()) : null);
            ps.setString(4, req.getSeverity());
            ps.setString(5, req.getNotes());
            return ps;
        }, keyHolder);

        return keyHolder.getKey().longValue();
    }

    @Override
    public List<ChronicCondition> findActiveConditionsByPatientId(Long patientId) {
        String sql = """
                SELECT id, patient_id, condition_name, diagnosed_date,
                       severity, is_active, notes, created_at
                FROM chronic_conditions
                WHERE patient_id = ? AND is_active = TRUE
                ORDER BY diagnosed_date DESC NULLS LAST
                """;

        return jdbc.query(sql, mapper::mapToChronicCondition, patientId);
    }

    @Override
    public void deactivateCondition(Long conditionId, Long patientId) {
        // Nu ștergem — marcăm ca inactivă (soft delete medical)
        // Istoricul medical trebuie păstrat conform cerințelor sistemului
        jdbc.update("""
                UPDATE chronic_conditions
                SET is_active = FALSE
                WHERE id = ? AND patient_id = ?
                """, conditionId, patientId);
    }
}