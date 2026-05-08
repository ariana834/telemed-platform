package com.telemedicina.shared.exception;

import org.springframework.http.HttpStatus;

public class AppointmentUnavailableException extends ApiException {
    public AppointmentUnavailableException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}