package com.telemedicina.patient.controller;

import com.telemedicina.patient.dto.request.ChronicConditionRequest;
import com.telemedicina.patient.dto.request.GuardianRequest;
import com.telemedicina.patient.dto.request.PatientRequest;
import com.telemedicina.patient.dto.response.ChronicConditionResponse;
import com.telemedicina.patient.dto.response.GuardianResponse;
import com.telemedicina.patient.dto.response.PatientResponse;
import com.telemedicina.patient.service.PatientService;
import com.telemedicina.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Toate endpoint-urile sunt sub /api/v1/patients.
 *
 * @AuthenticationPrincipal CustomUserDetails userDetails
 * — Spring Security injectează automat userul din JWT.
 * Folosim userDetails.getUserId() ca să nu acceptăm niciodată userId din body
 * (un user nu poate acționa în numele altui user).
 *
 * @PreAuthorize("hasRole('PATIENT')") — blochează accesul dacă rolul din JWT
 * nu e PATIENT. Doctori și admini nu pot crea profiluri de pacient.
 */
@RestController
@RequestMapping("/api/v1/patients")
@Tag(name = "Pacienți", description = "Managementul profilului de pacient")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    // ─── Profil ───────────────────────────────────────────────────────────────

    @PostMapping("/profile")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Creare profil pacient",
            description = "Un cont poate avea un singur profil. " +
                    "Vârsta și categoria de vârstă sunt calculate automat de DB.")
    public ResponseEntity<PatientResponse> createProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PatientRequest request) {

        PatientResponse response = patientService.createProfile(
                userDetails.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/profile")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Profil propriu",
            description = "Returnează profilul pacientului curent, inclusiv vârsta calculată.")
    public ResponseEntity<PatientResponse> getProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return ResponseEntity.ok(patientService.getProfile(userDetails.getUserId()));
    }

    @PutMapping("/profile")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Actualizare profil",
            description = "Dacă se modifică birth_date, age_category se recalculează automat.")
    public ResponseEntity<PatientResponse> updateProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PatientRequest request) {

        return ResponseEntity.ok(
                patientService.updateProfile(userDetails.getUserId(), request));
    }

    // ─── Guardian ─────────────────────────────────────────────────────────────

    @PostMapping("/{patientId}/guardian")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Adaugă tutore pentru un copil",
            description = "Userul curent devine tutorele pacientului copil specificat. " +
                    "DB-ul validează că patientId aparține unui pacient CHILD — " +
                    "altfel aruncă GUARDIAN_ONLY_FOR_CHILD (400).")
    public ResponseEntity<GuardianResponse> addGuardian(
            @PathVariable Long patientId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody GuardianRequest request) {

        GuardianResponse response = patientService.addGuardian(
                patientId, userDetails.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{patientId}/guardian")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Tutorele unui pacient copil")
    public ResponseEntity<GuardianResponse> getGuardian(
            @PathVariable Long patientId) {

        return ResponseEntity.ok(patientService.getGuardian(patientId));
    }

    // ─── Afecțiuni cronice ────────────────────────────────────────────────────

    @PostMapping("/chronic-conditions")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Adaugă afecțiune cronică",
            description = "Afecțiunile active sunt folosite de generate_medical_form() " +
                    "pentru a adapta întrebările fișei medicale. " +
                    "Nu pot exista duplicate active (constrângere DB).")
    public ResponseEntity<ChronicConditionResponse> addCondition(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ChronicConditionRequest request) {

        ChronicConditionResponse response = patientService.addChronicCondition(
                userDetails.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/chronic-conditions")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Afecțiunile cronice active ale pacientului curent")
    public ResponseEntity<List<ChronicConditionResponse>> getConditions(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return ResponseEntity.ok(
                patientService.getActiveConditions(userDetails.getUserId()));
    }

    @DeleteMapping("/chronic-conditions/{conditionId}")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Dezactivează afecțiune cronică",
            description = "Soft delete — înregistrarea rămâne în DB cu is_active=FALSE. " +
                    "Istoricul medical trebuie păstrat.")
    public ResponseEntity<Void> deactivateCondition(
            @PathVariable Long conditionId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        patientService.deactivateCondition(conditionId, userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }
}