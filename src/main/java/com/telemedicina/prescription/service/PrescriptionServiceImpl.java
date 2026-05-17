package com.telemedicina.prescription.service;

import com.telemedicina.patient.repository.PatientRepository;
import com.telemedicina.prescription.dto.request.CreatePrescriptionRequest;
import com.telemedicina.prescription.dto.request.CreateReferralRequest;
import com.telemedicina.prescription.dto.response.PrescriptionResponse;
import com.telemedicina.prescription.dto.response.ReferralResponse;
import com.telemedicina.prescription.mapper.PrescriptionMapper;
import com.telemedicina.prescription.model.Prescription;
import com.telemedicina.prescription.model.PrescriptionMedication;
import com.telemedicina.prescription.model.Referral;
import com.telemedicina.prescription.repository.PrescriptionRepository;
import com.telemedicina.shared.exception.ApiException;
import com.telemedicina.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PrescriptionServiceImpl implements PrescriptionService {

    private final PrescriptionRepository prescriptionRepo;
    private final PatientRepository patientRepo;
    private final PrescriptionMapper mapper;

    @Override
    public PrescriptionResponse getById(Long id, Long userId) {
        Prescription prescription = prescriptionRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reteta", id));

        verifyPatientOwnership(prescription.getPatientId(), userId);

        return buildPrescriptionResponse(prescription);
    }

    @Override
    public List<PrescriptionResponse> getMyPrescriptions(Long userId) {
        Long patientId = getPatientId(userId);

        return prescriptionRepo.findAllByPatientId(patientId).stream()
                .map(this::buildPrescriptionResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PrescriptionResponse getByConsultationId(Long consultationId, Long userId) {
        Prescription prescription = prescriptionRepo.findByConsultationId(consultationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reteta pentru consultatia", consultationId));

        verifyPatientOwnership(prescription.getPatientId(), userId);

        return buildPrescriptionResponse(prescription);
    }

    @Override
    @Transactional
    public PrescriptionResponse createPrescription(CreatePrescriptionRequest request, Long userId) {
        Long doctorId = getDoctorId(userId);
        Long patientId = getPatientIdFromConsultation(request.getConsultationId());

        // cream reteta
        Long prescriptionId = prescriptionRepo.createPrescription(
                request.getConsultationId(),
                patientId,
                doctorId,
                request.getDiagnosisId(),
                request.getValidDays(),
                false  // creat de doctor, nu automat
        );

        // adaugam fiecare medicament
        for (CreatePrescriptionRequest.MedicationItem med : request.getMedications()) {
            prescriptionRepo.addMedication(
                    prescriptionId,
                    med.getMedicationName(),
                    med.getDosage(),
                    med.getFrequency(),
                    med.getDurationDays(),
                    med.getInstructions()
            );
        }

        Prescription prescription = prescriptionRepo.findById(prescriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Reteta", prescriptionId));

        return buildPrescriptionResponse(prescription);
    }


    @Override
    public ReferralResponse getReferralById(Long id, Long userId) {
        Referral referral = prescriptionRepo.findReferralById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trimitere", id));

        verifyPatientOwnership(referral.getPatientId(), userId);

        return mapper.toReferralResponse(referral);
    }

    @Override
    public List<ReferralResponse> getMyReferrals(Long userId) {
        Long patientId = getPatientId(userId);

        return prescriptionRepo.findReferralsByPatientId(patientId).stream()
                .map(mapper::toReferralResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReferralResponse> getReferralsByConsultation(Long consultationId, Long userId) {
        // verificam ca pacientul are acces la aceasta consultatie
        Long patientId = getPatientId(userId);

        return prescriptionRepo.findReferralsByConsultationId(consultationId).stream()
                .filter(r -> r.getPatientId().equals(patientId))
                .map(mapper::toReferralResponse)
                .collect(Collectors.toList());
    }


    @Override
    @Transactional
    public ReferralResponse createReferral(CreateReferralRequest request, Long userId) {
        Long doctorId = getDoctorId(userId);
        Long patientId = getPatientIdFromConsultation(request.getConsultationId());

        Long referralId = prescriptionRepo.createReferral(
                request.getConsultationId(),
                patientId,
                doctorId,
                request.getReferralType().name(),
                request.getPriority().name(),
                request.getDestination(),
                request.getReason()
        );

        Referral referral = prescriptionRepo.findReferralById(referralId)
                .orElseThrow(() -> new ResourceNotFoundException("Trimitere", referralId));

        return mapper.toReferralResponse(referral);
    }


    // construieste response-ul complet: reteta + medicamentele ei
    private PrescriptionResponse buildPrescriptionResponse(Prescription p) {
        List<PrescriptionMedication> medications = prescriptionRepo.findMedications(p.getId());
        return mapper.toResponse(p, medications);
    }

    // returneaza patient_id pentru userul curent
    private Long getPatientId(Long userId) {
        return patientRepo.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Pacient", userId))
                .getId();
    }

    // verifica ca reteta/trimiterea apartine pacientului curent
    private void verifyPatientOwnership(Long resourcePatientId, Long userId) {
        Long currentPatientId = getPatientId(userId);
        if (!resourcePatientId.equals(currentPatientId)) {
            throw new ApiException("Nu ai acces la aceasta resursa.", HttpStatus.FORBIDDEN);
        }
    }

    // verifica ca userul e doctor si returneaza doctor_id
    private Long getDoctorId(Long userId) {
        return prescriptionRepo.findDoctorIdByUserId(userId)
                .orElseThrow(() -> new ApiException(
                        "Nu esti inregistrat ca doctor in sistem.", HttpStatus.FORBIDDEN
                ));
    }

    // gaseste patient_id-ul asociat unei consultatii
    private Long getPatientIdFromConsultation(Long consultationId) {
        return patientRepo.findPatientIdByConsultationId(consultationId)
                .orElseThrow(() -> new ResourceNotFoundException("Consultatie", consultationId));
    }
}