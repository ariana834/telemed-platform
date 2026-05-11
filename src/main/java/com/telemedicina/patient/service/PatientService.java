package com.telemedicina.patient.service;

import com.telemedicina.patient.dto.*;

import java.util.List;

public interface PatientService {
    PatientResponse createProfile(Long userId, PatientRequest request);
    PatientResponse getProfile(Long userId);
    PatientResponse updateProfile(Long userId, PatientRequest request);


    GuardianResponse addGuardian(Long patientId, Long guardianUserId, GuardianRequest request);
    GuardianResponse getGuardian(Long patientId);


    ChronicConditionResponse addChronicCondition(Long userId, ChronicConditionRequest request);
    List<ChronicConditionResponse> getActiveConditions(Long userId);
    void deactivateCondition(Long conditionId, Long userId);
}