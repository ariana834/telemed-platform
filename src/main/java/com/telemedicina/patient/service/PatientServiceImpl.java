package com.telemedicina.patient.service;

import com.telemedicina.patient.dto.request.ChronicConditionRequest;
import com.telemedicina.patient.dto.request.GuardianRequest;
import com.telemedicina.patient.dto.request.PatientRequest;
import com.telemedicina.patient.dto.response.ChronicConditionResponse;
import com.telemedicina.patient.dto.response.GuardianResponse;
import com.telemedicina.patient.dto.response.PatientResponse;
import com.telemedicina.patient.mapper.PatientMapper;
import com.telemedicina.patient.model.ChronicCondition;
import com.telemedicina.patient.model.Patient;
import com.telemedicina.patient.repository.PatientRepository;
import com.telemedicina.shared.exception.ApiException;
import com.telemedicina.shared.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;

    @Autowired
    public PatientServiceImpl(PatientRepository patientRepository,
                              PatientMapper patientMapper) {
        this.patientRepository = patientRepository;
        this.patientMapper = patientMapper;
    }

    @Override
    public PatientResponse createProfile(Long userId, PatientRequest request) {
        // Un user poate avea un singur profil de pacient
        if (patientRepository.existsByUserId(userId)) {
            throw new ApiException(
                    "Există deja un profil de pacient pentru acest cont", HttpStatus.CONFLICT);
        }

        Long patientId = patientRepository.createPatient(userId, request);

        return patientRepository.findById(patientId)
                .map(patientMapper::toResponse)
                .orElseThrow(() -> new ApiException("Eroare la crearea profilului", HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @Override
    public PatientResponse getProfile(Long userId) {
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException("Nu există un profil de pacient pentru acest cont", HttpStatus.NOT_FOUND));
        return patientMapper.toResponse(patient);
    }

    @Override
    public PatientResponse updateProfile(Long userId, PatientRequest request) {
        if (!patientRepository.existsByUserId(userId)) {
            throw new ApiException("Nu există un profil de pacient pentru acest cont", HttpStatus.NOT_FOUND);
        }

        patientRepository.updatePatient(userId, request);

        return patientRepository.findByUserId(userId)
                .map(patientMapper::toResponse)
                .orElseThrow();
    }


    @Override
    public GuardianResponse addGuardian(Long patientId, Long guardianUserId,
                                        GuardianRequest request) {
        patientRepository.findById(patientId).orElseThrow(() -> new ResourceNotFoundException("Pacient", patientId));

        patientRepository.createGuardian(patientId, guardianUserId, request);

        return patientRepository.findGuardianByPatientId(patientId)
                .map(patientMapper::toGuardianResponse)
                .orElseThrow();
    }

    @Override
    public GuardianResponse getGuardian(Long patientId) {
        return patientRepository.findGuardianByPatientId(patientId)
                .map(patientMapper::toGuardianResponse)
                .orElseThrow(() -> new ApiException(
                        "Nu există tutore înregistrat pentru pacientul cu id " + patientId,
                        HttpStatus.NOT_FOUND));
    }

    @Override
    public ChronicConditionResponse addChronicCondition(Long userId,
                                                        ChronicConditionRequest request) {
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(
                        "Creează mai întâi un profil de pacient", HttpStatus.NOT_FOUND));

        Long conditionId = patientRepository.createChronicCondition(patient.getId(), request);

        List<ChronicCondition> conditions = patientRepository.findActiveConditionsByPatientId(patient.getId());

        return conditions.stream()
                .filter(c -> c.getId().equals(conditionId))
                .map(patientMapper::toChronicConditionResponse)
                .findFirst()
                .orElseThrow();
    }

    @Override
    public List<ChronicConditionResponse> getActiveConditions(Long userId) {
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(
                        "Nu există un profil de pacient pentru acest cont", HttpStatus.NOT_FOUND));

        return patientRepository.findActiveConditionsByPatientId(patient.getId())
                .stream()
                .map(patientMapper::toChronicConditionResponse)
                .toList();
    }

    @Override
    public void deactivateCondition(Long conditionId, Long userId) {
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException("Nu există un profil de pacient pentru acest cont", HttpStatus.NOT_FOUND));

        patientRepository.deactivateCondition(conditionId, patient.getId());
    }
}