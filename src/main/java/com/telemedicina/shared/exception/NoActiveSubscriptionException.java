package com.telemedicina.shared.exception;

import org.springframework.http.HttpStatus;

public class NoActiveSubscriptionException extends ApiException {
    public NoActiveSubscriptionException(Long patientId) {
        super("Pacientul " + patientId + " nu are un abonament activ", HttpStatus.FORBIDDEN);
    }
}