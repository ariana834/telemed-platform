package com.telemedicina.prescription.controller;

import com.telemedicina.prescription.dto.request.CreatePrescriptionRequest;
import com.telemedicina.prescription.dto.request.CreateReferralRequest;
import com.telemedicina.prescription.dto.response.PrescriptionResponse;
import com.telemedicina.prescription.dto.response.ReferralResponse;
import com.telemedicina.prescription.service.PrescriptionService;
import com.telemedicina.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PrescriptionController {

    private final PrescriptionService service;

    // toate retetele pacientului curent, ordonate dupa data
    @GetMapping("/api/v1/prescriptions/my")
    public ResponseEntity<List<PrescriptionResponse>> getMyPrescriptions(
            @AuthenticationPrincipal CustomUserDetails user) {

        return ResponseEntity.ok(service.getMyPrescriptions(user.getUserId()));
    }

    // detalii reteta (include automat medicamentele)
    @GetMapping("/api/v1/prescriptions/{id}")
    public ResponseEntity<PrescriptionResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user) {

        return ResponseEntity.ok(service.getById(id, user.getUserId()));
    }

    // reteta asociata unei consultatii - util cand pacientul termina consultatia
    @GetMapping("/api/v1/prescriptions/consultation/{consultationId}")
    public ResponseEntity<PrescriptionResponse> getByConsultation(
            @PathVariable Long consultationId,
            @AuthenticationPrincipal CustomUserDetails user) {

        return ResponseEntity.ok(service.getByConsultationId(consultationId, user.getUserId()));
    }

    // doctor creeaza o reteta manual dupa consultatia cu pacientul
    @PostMapping("/api/v1/prescriptions")
    public ResponseEntity<PrescriptionResponse> createPrescription(
            @RequestBody @Valid CreatePrescriptionRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.createPrescription(request, user.getUserId()));
    }

    // toate trimiterile pacientului curent
    @GetMapping("/api/v1/referrals/my")
    public ResponseEntity<List<ReferralResponse>> getMyReferrals(
            @AuthenticationPrincipal CustomUserDetails user) {

        return ResponseEntity.ok(service.getMyReferrals(user.getUserId()));
    }

    // detalii trimitere
    @GetMapping("/api/v1/referrals/{id}")
    public ResponseEntity<ReferralResponse> getReferralById(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user) {

        return ResponseEntity.ok(service.getReferralById(id, user.getUserId()));
    }

    // trimiterile unei consultatii specifice
    @GetMapping("/api/v1/referrals/consultation/{consultationId}")
    public ResponseEntity<List<ReferralResponse>> getReferralsByConsultation(
            @PathVariable Long consultationId,
            @AuthenticationPrincipal CustomUserDetails user) {

        return ResponseEntity.ok(service.getReferralsByConsultation(consultationId, user.getUserId()));
    }

    // doctor creeaza o trimitere catre spital sau investigatii
    @PostMapping("/api/v1/referrals")
    public ResponseEntity<ReferralResponse> createReferral(
            @RequestBody @Valid CreateReferralRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.createReferral(request, user.getUserId()));
    }
}