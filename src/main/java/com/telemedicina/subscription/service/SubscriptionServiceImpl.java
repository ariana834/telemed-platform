package com.telemedicina.subscription.service;

import com.telemedicina.patient.model.Patient;
import com.telemedicina.patient.repository.PatientRepository;
import com.telemedicina.shared.exception.ApiException;
import com.telemedicina.shared.exception.ResourceNotFoundException;
import com.telemedicina.subscription.dto.request.PaymentRequest;
import com.telemedicina.subscription.dto.request.SubscriptionRequest;
import com.telemedicina.subscription.dto.response.PaymentResponse;
import com.telemedicina.subscription.dto.response.SubscriptionResponse;
import com.telemedicina.subscription.mapper.SubscriptionMapper;
import com.telemedicina.subscription.model.Subscription;
import com.telemedicina.subscription.repository.SubscriptionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final PatientRepository patientRepository;
    private final SubscriptionMapper mapper;

    public SubscriptionServiceImpl(SubscriptionRepository subscriptionRepository,
                                   PatientRepository patientRepository,
                                   SubscriptionMapper mapper) {
        this.subscriptionRepository = subscriptionRepository;
        this.patientRepository = patientRepository;
        this.mapper = mapper;
    }

    // helper ca sa nu repet asta peste tot
    private Patient getPatientOrThrow(Long userId) {
        return patientRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(
                        "Creează mai întâi un profil de pacient", HttpStatus.NOT_FOUND));
    }

    @Override
    public SubscriptionResponse createSubscription(Long userId, SubscriptionRequest request) {
        Patient patient = getPatientOrThrow(userId);

        // nu permitem un al doilea abonament activ
        // constrângerea uq_one_active_subscription din DB prinde asta oricum,
        // dar e mai frumos sa dam un mesaj clar
        subscriptionRepository.findActiveByPatientId(patient.getId()).ifPresent(s -> {
            throw new ApiException("Ai deja un abonament activ", HttpStatus.CONFLICT);
        });

        Long id = subscriptionRepository.createSubscription(patient.getId(), request);

        return subscriptionRepository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow();
    }

    @Override
    public SubscriptionResponse getActiveSubscription(Long userId) {
        Patient patient = getPatientOrThrow(userId);

        return subscriptionRepository.findActiveByPatientId(patient.getId())
                .map(mapper::toResponse)
                .orElseThrow(() -> new ApiException(
                        "Nu ai niciun abonament activ", HttpStatus.NOT_FOUND));
    }

    @Override
    public List<SubscriptionResponse> getAllSubscriptions(Long userId) {
        Patient patient = getPatientOrThrow(userId);

        return subscriptionRepository.findAllByPatientId(patient.getId())
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public PaymentResponse pay(Long userId, Long subscriptionId, PaymentRequest request) {
        Patient patient = getPatientOrThrow(userId);

        // verificam ca abonamentul apartine pacientului curent
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Abonament", subscriptionId));

        if (!subscription.getPatientId().equals(patient.getId())) {
            throw new ApiException("Nu ai acces la acest abonament", HttpStatus.FORBIDDEN);
        }

        if (!subscription.getStatus().equals("PENDING")) {
            throw new ApiException(
                    "Abonamentul nu mai poate fi platit (status: " + subscription.getStatus() + ")",
                    HttpStatus.BAD_REQUEST);
        }

        // cream plata PENDING, apoi o confirmam imediat
        // triggerul trg_activate_subscription_on_payment prinde confirmarea
        // si seteaza automat abonamentul pe ACTIVE
        Long paymentId = subscriptionRepository.createPayment(subscriptionId, request);
        subscriptionRepository.confirmPayment(paymentId);

        return subscriptionRepository.findPaymentById(paymentId)
                .map(mapper::toPaymentResponse)
                .orElseThrow();
    }

    @Override
    public List<PaymentResponse> getPaymentHistory(Long userId, Long subscriptionId) {
        Patient patient = getPatientOrThrow(userId);

        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Abonament", subscriptionId));

        if (!subscription.getPatientId().equals(patient.getId())) {
            throw new ApiException("Nu ai acces la acest abonament", HttpStatus.FORBIDDEN);
        }

        return subscriptionRepository.findPaymentsBySubscriptionId(subscriptionId)
                .stream()
                .map(mapper::toPaymentResponse)
                .toList();
    }
}