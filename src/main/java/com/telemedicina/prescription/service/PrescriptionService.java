package com.telemedicina.prescription.service;

import com.telemedicina.prescription.dto.request.CreatePrescriptionRequest;
import com.telemedicina.prescription.dto.request.CreateReferralRequest;
import com.telemedicina.prescription.dto.response.PrescriptionResponse;
import com.telemedicina.prescription.dto.response.ReferralResponse;

import java.util.List;

public interface PrescriptionService {

    // ---- pacient ----
    PrescriptionResponse getById(Long id, Long userId);
    List<PrescriptionResponse> getMyPrescriptions(Long userId);
    PrescriptionResponse getByConsultationId(Long consultationId, Long userId);

    // ---- doctor ----
    PrescriptionResponse createPrescription(CreatePrescriptionRequest request, Long userId);

    // ---- trimiteri - pacient ----
    ReferralResponse getReferralById(Long id, Long userId);
    List<ReferralResponse> getMyReferrals(Long userId);
    List<ReferralResponse> getReferralsByConsultation(Long consultationId, Long userId);

    // ---- trimiteri - doctor ----
    ReferralResponse createReferral(CreateReferralRequest request, Long userId);
}