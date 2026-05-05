package com.telemedicina.shared.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;
import java.util.List;


@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(org.springframework.jdbc.UncategorizedSQLException.class)
    public ResponseEntity<ApiError> handleUncategorizedSQL(
            org.springframework.jdbc.UncategorizedSQLException ex,
            HttpServletRequest request) {

        String message = extractPgMessage(ex);
        log.warn("Excepție SQL neîncadrată: {}", message);

        //pacient fără abonament activ încearcă să creeze consultație
        if (message.contains("NO_ACTIVE_SUBSCRIPTION")) {
            String detail = message.replace("NO_ACTIVE_SUBSCRIPTION:", "").trim();
            return buildResponse(new NoActiveSubscriptionException(detail), request);
        }

        //tutore adăugat pentru pacient adult
        if (message.contains("GUARDIAN_ONLY_FOR_CHILD")) {
            String detail = message.replace("GUARDIAN_ONLY_FOR_CHILD:", "").trim();
            return buildError(HttpStatus.BAD_REQUEST, "Date invalide", detail, request.getRequestURI());
        }

        if (message.contains("NO_SLOTS_AVAILABLE")) {
            String detail = message.replace("NO_SLOTS_AVAILABLE:", "").trim();
            return buildResponse(new AppointmentUnavailableException(detail), request);
        }

        if (message.contains("MAX_SYMPTOMS_REACHED")) {
            return buildError(HttpStatus.BAD_REQUEST, "Simptome invalide",
                    "Maximum 3 simptome sunt permise per consultație.", request.getRequestURI());
        }

        if (message.contains("INVALID_STATUS_TRANSITION")) {
            String detail = message.replace("INVALID_STATUS_TRANSITION:", "").trim();
            return buildError(HttpStatus.CONFLICT, "Tranziție invalidă", detail, request.getRequestURI());
        }

        if (message.contains("EMERGENCY_NO_APPOINTMENT")) {
            return buildError(HttpStatus.BAD_REQUEST, "Caz de urgență",
                    "Cazurile de urgență necesită prezentare directă la spital.", request.getRequestURI());
        }

        if (message.contains("DIAGNOSIS_NOT_ELIGIBLE")) {
            String detail = message.replace("DIAGNOSIS_NOT_ELIGIBLE:", "").trim();
            return buildError(HttpStatus.BAD_REQUEST, "Rețetă automată indisponibilă", detail, request.getRequestURI());
        }

        // Eroare SQL necunoscută — logăm complet pentru debugging
        log.error("Excepție SQL necunoscută", ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Eroare server",
                "Eroare internă la procesarea cererii.", request.getRequestURI());
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .toList();

        ApiError error = new ApiError(400, "Date invalide",
                "Validarea datelor a eșuat.", request.getRequestURI());
        error.setDetails(details);

        return ResponseEntity.badRequest().body(error);
    }

    /** Excepțiile noastre custom de business */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApiException(ApiException ex, HttpServletRequest request) {
        return buildResponse(ex, request);
    }

    /** Credențiale greșite la login */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(HttpServletRequest request) {
        return buildError(HttpStatus.UNAUTHORIZED, "Autentificare eșuată",
                "Email sau parolă incorectă.", request.getRequestURI());
    }

    /** Acces interzis (utilizator autentificat dar fără permisiuni) */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(HttpServletRequest request) {
        return buildError(HttpStatus.FORBIDDEN, "Acces interzis",
                "Nu ai permisiunile necesare pentru această acțiune.", request.getRequestURI());
    }

    /** Violări de constrângeri DB (UNIQUE, FK) */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Violație constrângere DB: {}", ex.getMessage());
        return buildError(HttpStatus.CONFLICT, "Conflict date",
                "Datele introduse încalcă o constrângere a bazei de date.", request.getRequestURI());
    }

    /** Fallback — orice altă excepție neașteptată */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneral(Exception ex, HttpServletRequest request) {
        log.error("Excepție neașteptată la {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Eroare server",
                "A apărut o eroare internă. Contactați administratorul.", request.getRequestURI());
    }



    private ResponseEntity<ApiError> buildResponse(ApiException ex, HttpServletRequest request) {
        return buildError(ex.getStatus(), ex.getStatus().getReasonPhrase(),
                ex.getMessage(), request.getRequestURI());
    }

    private ResponseEntity<ApiError> buildError(HttpStatus status, String error,
                                                String message, String path) {
        ApiError apiError = new ApiError(status.value(), error, message, path);
        return ResponseEntity.status(status).body(apiError);
    }

    /** Extrage mesajul din excepțiile JDBC care înfășoară SQLException */
    private String extractPgMessage(Exception ex) {
        Throwable cause = ex;
        while (cause != null) {
            if (cause instanceof SQLException sqlEx) {
                String msg = sqlEx.getMessage();
                if (msg != null) return msg;
            }
            cause = cause.getCause();
        }
        return ex.getMessage() != null ? ex.getMessage() : "Eroare necunoscută";
    }
}