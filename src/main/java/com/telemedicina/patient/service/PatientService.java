package com.telemedicina.patient.service;

import com.telemedicina.patient.dto.request.ChronicConditionRequest;
import com.telemedicina.patient.dto.request.GuardianRequest;
import com.telemedicina.patient.dto.request.PatientRequest;
import com.telemedicina.patient.dto.response.ChronicConditionResponse;
import com.telemedicina.patient.dto.response.GuardianResponse;
import com.telemedicina.patient.dto.response.PatientResponse;

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