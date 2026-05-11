package com.telemedicina.patient.mapper;

import com.telemedicina.patient.dto.response.ChronicConditionResponse;
import com.telemedicina.patient.dto.response.GuardianResponse;
import com.telemedicina.patient.dto.response.PatientResponse;
import com.telemedicina.patient.model.ChronicCondition;
import com.telemedicina.patient.model.Guardian;
import com.telemedicina.patient.model.Patient;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Mapper manual — fără ORM.
 * Fiecare metodă mapează un ResultSet la modelul Java corespunzător,
 * sau un model la DTO-ul de răspuns.
 *
 * ResultSet → Model: folosit în repository (JdbcTemplate RowMapper)
 * Model → DTO:       folosit în service înainte să returneze răspunsul
 */
@Component
public class PatientMapper {

    // ─── ResultSet → Patient ──────────────────────────────────────────────────

    public Patient mapToPatient(ResultSet rs, int rowNum) throws SQLException {
        Patient p = new Patient();
        p.setId(rs.getLong("id"));
        p.setUserId(rs.getLong("user_id"));
        p.setFirstName(rs.getString("first_name"));
        p.setLastName(rs.getString("last_name"));

        // birth_date vine ca java.sql.Date, convertim la LocalDate
        var bd = rs.getDate("birth_date");
        if (bd != null) p.setBirthDate(bd.toLocalDate());

        p.setAge(rs.getObject("age", Integer.class));
        p.setAgeCategory(rs.getString("age_category"));
        p.setGender(rs.getString("gender"));
        p.setBloodType(rs.getString("blood_type"));
        p.setPhone(rs.getString("phone"));
        p.setCnp(rs.getString("cnp"));
        p.setAddress(rs.getString("address"));
        p.setGuardian(rs.getBoolean("is_guardian"));

        // TIMESTAMPTZ → OffsetDateTime
        var createdAt = rs.getObject("created_at", java.time.OffsetDateTime.class);
        p.setCreatedAt(createdAt);
        var updatedAt = rs.getObject("updated_at", java.time.OffsetDateTime.class);
        p.setUpdatedAt(updatedAt);

        return p;
    }

    // ─── Patient → PatientResponse ────────────────────────────────────────────

    public PatientResponse toResponse(Patient p) {
        PatientResponse r = new PatientResponse();
        r.setId(p.getId());
        r.setUserId(p.getUserId());
        r.setFirstName(p.getFirstName());
        r.setLastName(p.getLastName());
        r.setBirthDate(p.getBirthDate());
        r.setAge(p.getAge());
        r.setAgeCategory(p.getAgeCategory());
        r.setGender(p.getGender());
        r.setBloodType(p.getBloodType());
        r.setPhone(p.getPhone());
        r.setCnp(p.getCnp());
        r.setAddress(p.getAddress());
        r.setGuardian(p.isGuardian());
        r.setCreatedAt(p.getCreatedAt());
        return r;
    }

    // ─── ResultSet → Guardian ─────────────────────────────────────────────────

    public Guardian mapToGuardian(ResultSet rs, int rowNum) throws SQLException {
        Guardian g = new Guardian();
        g.setId(rs.getLong("id"));
        g.setPatientId(rs.getLong("patient_id"));
        g.setGuardianUserId(rs.getLong("guardian_user_id"));
        g.setFirstName(rs.getString("first_name"));
        g.setLastName(rs.getString("last_name"));
        g.setPhone(rs.getString("phone"));
        g.setEmail(rs.getString("email"));
        g.setRelationship(rs.getString("relationship"));
        g.setCreatedAt(rs.getObject("created_at", java.time.OffsetDateTime.class));
        return g;
    }

    // ─── Guardian → GuardianResponse ─────────────────────────────────────────

    public GuardianResponse toGuardianResponse(Guardian g) {
        GuardianResponse r = new GuardianResponse();
        r.setId(g.getId());
        r.setPatientId(g.getPatientId());
        r.setGuardianUserId(g.getGuardianUserId());
        r.setFirstName(g.getFirstName());
        r.setLastName(g.getLastName());
        r.setPhone(g.getPhone());
        r.setEmail(g.getEmail());
        r.setRelationship(g.getRelationship());
        r.setCreatedAt(g.getCreatedAt());
        return r;
    }

    // ─── ResultSet → ChronicCondition ────────────────────────────────────────

    public ChronicCondition mapToChronicCondition(ResultSet rs, int rowNum) throws SQLException {
        ChronicCondition cc = new ChronicCondition();
        cc.setId(rs.getLong("id"));
        cc.setPatientId(rs.getLong("patient_id"));
        cc.setConditionName(rs.getString("condition_name"));

        var dd = rs.getDate("diagnosed_date");
        if (dd != null) cc.setDiagnosedDate(dd.toLocalDate());

        cc.setSeverity(rs.getString("severity"));
        cc.setActive(rs.getBoolean("is_active"));
        cc.setNotes(rs.getString("notes"));
        cc.setCreatedAt(rs.getObject("created_at", java.time.OffsetDateTime.class));
        return cc;
    }

    // ─── ChronicCondition → ChronicConditionResponse ─────────────────────────

    public ChronicConditionResponse toChronicConditionResponse(ChronicCondition cc) {
        ChronicConditionResponse r = new ChronicConditionResponse();
        r.setId(cc.getId());
        r.setPatientId(cc.getPatientId());
        r.setConditionName(cc.getConditionName());
        r.setDiagnosedDate(cc.getDiagnosedDate());
        r.setSeverity(cc.getSeverity());
        r.setActive(cc.isActive());
        r.setNotes(cc.getNotes());
        r.setCreatedAt(cc.getCreatedAt());
        return r;
    }
}