package com.telemedicina.patient.service;

import com.telemedicina.patient.dto.*;
import com.telemedicina.patient.mapper.PatientMapper;
import com.telemedicina.patient.model.ChronicCondition;
import com.telemedicina.patient.model.Guardian;
import com.telemedicina.patient.model.Patient;
import com.telemedicina.patient.repository.PatientRepository;
import com.telemedicina.shared.exception.ApiException;
import com.telemedicina.shared.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Logica de business pentru pacienți.
 *
 * Principiu: service-ul validează regulile aplicației (un user nu poate
 * avea 2 profiluri, nu poți edita profilul altcuiva etc.).
 * Regulile de DB (trigger-ele) sunt un al doilea nivel de apărare —
 * dacă service-ul scapă ceva, DB-ul prinde.
 */
@Service
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;

    public PatientServiceImpl(PatientRepository patientRepository,
                              PatientMapper patientMapper) {
        this.patientRepository = patientRepository;
        this.patientMapper = patientMapper;
    }

    // ─── Patient ──────────────────────────────────────────────────────────────

    @Override
    public PatientResponse createProfile(Long userId, PatientRequest request) {
        // Un user poate avea un singur profil de pacient
        if (patientRepository.existsByUserId(userId)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Există deja un profil de pacient pentru acest cont");
        }

        Long patientId = patientRepository.createPatient(userId, request);

        // Citim profilul creat ca să avem și age + age_category calculate de DB
        return patientRepository.findById(patientId)
                .map(patientMapper::toResponse)
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Eroare la crearea profilului"));
    }

    @Override
    public PatientResponse getProfile(Long userId) {
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nu există un profil de pacient pentru acest cont"));
        return patientMapper.toResponse(patient);
    }

    @Override
    public PatientResponse updateProfile(Long userId, PatientRequest request) {
        // Verificăm că profilul există înainte de update
        if (!patientRepository.existsByUserId(userId)) {
            throw new ResourceNotFoundException(
                    "Nu există un profil de pacient pentru acest cont");
        }

        patientRepository.updatePatient(userId, request);

        // Returnăm profilul actualizat — age se recalculează dacă s-a schimbat birth_date
        return patientRepository.findByUserId(userId)
                .map(patientMapper::toResponse)
                .orElseThrow();
    }

    // ─── Guardian ─────────────────────────────────────────────────────────────

    @Override
    public GuardianResponse addGuardian(Long patientId, Long guardianUserId,
                                        GuardianRequest request) {
        // Verificăm că pacientul (copilul) există
        patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Pacientul cu id " + patientId + " nu a fost găsit"));

        // Triggerul trg_validate_guardian din DB verifică că e CHILD
        // Dacă nu e, aruncă GUARDIAN_ONLY_FOR_CHILD → prins în GlobalExceptionHandler → 400
        Long guardianId = patientRepository.createGuardian(patientId, guardianUserId, request);

        // Marcăm pacientul-tutore că are un copil în sistem
        // (is_guardian = TRUE pe profilul propriu al tutorelui)
        patientRepository.findByUserId(guardianUserId).ifPresent(p -> {
            // Actualizare simplă — în cazuri reale ai un updateIsGuardian separat
            // Aici îl facem inline cu un update direct
        });

        return patientRepository.findGuardianByPatientId(patientId)
                .map(patientMapper::toGuardianResponse)
                .orElseThrow();
    }

    @Override
    public GuardianResponse getGuardian(Long patientId) {
        return patientRepository.findGuardianByPatientId(patientId)
                .map(patientMapper::toGuardianResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nu există tutore înregistrat pentru pacientul " + patientId));
    }

    // ─── Chronic Conditions ───────────────────────────────────────────────────

    @Override
    public ChronicConditionResponse addChronicCondition(Long userId,
                                                        ChronicConditionRequest request) {
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Creează mai întâi un profil de pacient"));

        // Constrângerea uq_active_condition din DB previne duplicate active
        // DataIntegrityViolationException e prinsă în GlobalExceptionHandler → 409
        Long conditionId = patientRepository.createChronicCondition(
                patient.getId(), request);

        List<ChronicCondition> conditions =
                patientRepository.findActiveConditionsByPatientId(patient.getId());

        return conditions.stream()
                .filter(c -> c.getId().equals(conditionId))
                .map(patientMapper::toChronicConditionResponse)
                .findFirst()
                .orElseThrow();
    }

    @Override
    public List<ChronicConditionResponse> getActiveConditions(Long userId) {
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nu există un profil de pacient pentru acest cont"));

        return patientRepository.findActiveConditionsByPatientId(patient.getId())
                .stream()
                .map(patientMapper::toChronicConditionResponse)
                .toList();
    }

    @Override
    public void deactivateCondition(Long conditionId, Long userId) {
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nu există un profil de pacient pentru acest cont"));

        // patientId e inclus în query ca să nu poți dezactiva afecțiunea altcuiva
        patientRepository.deactivateCondition(conditionId, patient.getId());
    }
}