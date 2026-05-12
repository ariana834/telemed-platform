package com.telemedicina.consultation.controller;

import com.telemedicina.consultation.dto.request.FormAnswersRequest;
import com.telemedicina.consultation.dto.request.SymptomRequest;
import com.telemedicina.consultation.dto.response.ConsultationDetailResponse;
import com.telemedicina.consultation.dto.response.ConsultationResponse;
import com.telemedicina.consultation.dto.response.MedicalFormResponse;
import com.telemedicina.consultation.service.ConsultationService;
import com.telemedicina.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Fluxul normal al unei consultatii:
 * 1. POST /api/consultations              -> creeaza consultatia
 * 2. POST /api/consultations/{id}/symptoms (x3) -> adauga simptomele
 * 3. GET  /api/consultations/{id}/form    -> preia fisa generata de DB
 * 4. POST /api/consultations/{id}/answers -> trimite raspunsurile + primesti diagnosticul
 * 5a. POST /api/consultations/{id}/prescribe -> reteta automata (SIMPLE)
 * 5b. POST /api/consultations/{id}/schedule  -> programare la doctor (MEDIUM/COMPLEX)
 */
@RestController
@RequestMapping("/api/consultations")
@RequiredArgsConstructor
public class ConsultationController {

    private final ConsultationService service;

    // creeaza o consultatie noua
    // DB verifica automat abonamentul activ - daca nu are, returneaza 403
    @PostMapping
    public ResponseEntity<ConsultationResponse> create(
            @RequestParam(required = false) String notes,
            @AuthenticationPrincipal CustomUserDetails user) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.createConsultation(user.getUserId(), notes));
    }

    // istoricul consultatiilor pacientului curent, ordonate dupa data
    @GetMapping("/my")
    public ResponseEntity<List<ConsultationResponse>> getMyConsultations(
            @AuthenticationPrincipal CustomUserDetails user) {

        return ResponseEntity.ok(service.getMyConsultations(user.getUserId()));
    }

    // detalii complete: simptome + intrebari fisa + diagnostice
    @GetMapping("/{id}")
    public ResponseEntity<ConsultationDetailResponse> getConsultation(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user) {

        return ResponseEntity.ok(service.getConsultation(id, user.getUserId()));
    }

    // adauga un simptom (1, 2 sau 3)
    // dupa ultimul simptom, DB genereaza automat fisa medicala si trece la FORM_GENERATED
    // daca simptomele indica urgenta (durere abdominala severa + varsaturi), trece la EMERGENCY_REDIRECT
    @PostMapping("/{id}/symptoms")
    public ResponseEntity<ConsultationResponse> addSymptom(
            @PathVariable Long id,
            @RequestBody @Valid SymptomRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {

        return ResponseEntity.ok(service.addSymptom(id, user.getUserId(), request));
    }

    // preia fisa medicala generata de DB - intrebarile adaptate simptomelor si istoricului pacientului
    @GetMapping("/{id}/form")
    public ResponseEntity<List<MedicalFormResponse>> getForm(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user) {

        return ResponseEntity.ok(service.getForm(id, user.getUserId()));
    }

    // trimite toate raspunsurile la fisa
    // daca fisa este completa, calculam automat diagnosticul si il returnam in raspuns
    // pacientul primeste direct diagnosticul si poate decide ce face mai departe
    @PostMapping("/{id}/answers")
    public ResponseEntity<ConsultationDetailResponse> submitAnswers(
            @PathVariable Long id,
            @RequestBody @Valid FormAnswersRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {

        return ResponseEntity.ok(service.submitAnswers(id, user.getUserId(), request));
    }

    // programeaza la doctor - disponibil pentru MEDIUM si COMPLEX
    // DB cauta primul slot liber in 7 zile, din 10 in 10 minute, la toti doctorii disponibili
    // returneaza appointment_id-ul creat
    @PostMapping("/{id}/schedule")
    public ResponseEntity<Map<String, Long>> schedule(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user) {

        Long appointmentId = service.scheduleAppointment(id, user.getUserId());
        return ResponseEntity.ok(Map.of("appointmentId", appointmentId));
    }

    // reteta automata OTC - disponibila DOAR pentru cazuri SIMPLE
    // DB genereaza reteta cu medicamente uzuale (fara antibiotice) in functie de diagnostic
    // returneaza prescription_id-ul creat
    @PostMapping("/{id}/prescribe")
    public ResponseEntity<Map<String, Long>> prescribe(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user) {

        Long prescriptionId = service.generatePrescription(id, user.getUserId());
        return ResponseEntity.ok(Map.of("prescriptionId", prescriptionId));
    }

    @PostMapping("/{id}/diagnose")
    public ResponseEntity<ConsultationDetailResponse> diagnose(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user) {

        return ResponseEntity.ok(service.computeDiagnosis(id, user.getUserId()));
    }
}