package com.telemedicina.subscription.controller;

import com.telemedicina.security.CustomUserDetails;
import com.telemedicina.subscription.dto.request.PaymentRequest;
import com.telemedicina.subscription.dto.request.SubscriptionRequest;
import com.telemedicina.subscription.dto.response.PaymentResponse;
import com.telemedicina.subscription.dto.response.SubscriptionResponse;
import com.telemedicina.subscription.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subscriptions")
@Tag(name = "Abonamente", description = "Managementul abonamentelor medicale")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Creare abonament",
            description = "end_date și prețul sunt calculate automat de DB. MONTHLY = 50 RON, ANNUAL = 500 RON.")
    public ResponseEntity<SubscriptionResponse> create(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody SubscriptionRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subscriptionService.createSubscription(userDetails.getUserId(), request));
    }

    @GetMapping("/active")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Abonamentul activ curent")
    public ResponseEntity<SubscriptionResponse> getActive(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return ResponseEntity.ok(subscriptionService.getActiveSubscription(userDetails.getUserId()));
    }

    @GetMapping
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Toate abonamentele pacientului")
    public ResponseEntity<List<SubscriptionResponse>> getAll(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return ResponseEntity.ok(subscriptionService.getAllSubscriptions(userDetails.getUserId()));
    }

    @PostMapping("/{subscriptionId}/pay")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Plătește abonamentul",
            description = "Creează plata și o confirmă imediat. " +
                    "Triggerul din DB activează automat abonamentul după confirmare.")
    public ResponseEntity<PaymentResponse> pay(
            @PathVariable Long subscriptionId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PaymentRequest request) {

        return ResponseEntity.ok(
                subscriptionService.pay(userDetails.getUserId(), subscriptionId, request));
    }

    @GetMapping("/{subscriptionId}/payments")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Istoricul plăților pentru un abonament")
    public ResponseEntity<List<PaymentResponse>> getPayments(
            @PathVariable Long subscriptionId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return ResponseEntity.ok(
                subscriptionService.getPaymentHistory(userDetails.getUserId(), subscriptionId));
    }
}