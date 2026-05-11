package com.telemedicina.subscription.service;

import com.telemedicina.subscription.dto.request.PaymentRequest;
import com.telemedicina.subscription.dto.request.SubscriptionRequest;
import com.telemedicina.subscription.dto.response.PaymentResponse;
import com.telemedicina.subscription.dto.response.SubscriptionResponse;

import java.util.List;

public interface SubscriptionService {
    SubscriptionResponse createSubscription(Long userId, SubscriptionRequest request);
    SubscriptionResponse getActiveSubscription(Long userId);
    List<SubscriptionResponse> getAllSubscriptions(Long userId);

    // creeaza plata + confirma imediat → abonamentul devine ACTIVE
    PaymentResponse pay(Long userId, Long subscriptionId, PaymentRequest request);
    List<PaymentResponse> getPaymentHistory(Long userId, Long subscriptionId);
}