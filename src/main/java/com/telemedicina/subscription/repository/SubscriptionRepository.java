package com.telemedicina.subscription.repository;

import com.telemedicina.subscription.dto.request.PaymentRequest;
import com.telemedicina.subscription.dto.request.SubscriptionRequest;
import com.telemedicina.subscription.model.PaymentHistory;
import com.telemedicina.subscription.model.Subscription;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository {

    Long createSubscription(Long patientId, SubscriptionRequest request);
    Optional<Subscription> findById(Long id);
    Optional<Subscription> findActiveByPatientId(Long patientId);
    List<Subscription> findAllByPatientId(Long patientId);

    // creeaza o plata in stare PENDING, apoi o confirma imediat → triggerul activeaza abonamentul
    Long createPayment(Long subscriptionId, PaymentRequest request);
    void confirmPayment(Long paymentId);
    Optional<PaymentHistory> findPaymentById(Long paymentId);
    List<PaymentHistory> findPaymentsBySubscriptionId(Long subscriptionId);
}