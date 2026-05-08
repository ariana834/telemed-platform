package com.telemedicina.shared.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ApiException {
    public ResourceNotFoundException(String resource, Long id) {
        super(resource + " cu id " + id + " nu a fost găsit", HttpStatus.NOT_FOUND);
    }
}