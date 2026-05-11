package com.telemedicina.patient.repository;

import com.telemedicina.patient.dto.request.ChronicConditionRequest;
import com.telemedicina.patient.dto.request.GuardianRequest;
import com.telemedicina.patient.dto.request.PatientRequest;
import com.telemedicina.patient.model.ChronicCondition;
import com.telemedicina.patient.model.Guardian;
import com.telemedicina.patient.model.Patient;

import java.util.List;
import java.util.Optional;

public interface PatientRepository {

    // ─── Patient ──────────────────────────────────────────────────────────────
    Long createPatient(Long userId, PatientRequest request);
    Optional<Patient> findByUserId(Long userId);
    Optional<Patient> findById(Long patientId);
    void updatePatient(Long userId, PatientRequest request);
    boolean existsByUserId(Long userId);

    // ─── Guardian ─────────────────────────────────────────────────────────────
    Long createGuardian(Long patientId, Long guardianUserId, GuardianRequest request);
    Optional<Guardian> findGuardianByPatientId(Long patientId);

    // ─── Chronic Conditions ───────────────────────────────────────────────────
    Long createChronicCondition(Long patientId, ChronicConditionRequest request);
    List<ChronicCondition> findActiveConditionsByPatientId(Long patientId);
    void deactivateCondition(Long conditionId, Long patientId);
}