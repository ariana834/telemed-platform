package com.telemedicina.shared.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    //exceptiile facute de mine
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApiException(ApiException ex) {
        log.warn("ApiException: {}", ex.getMessage());
        ApiError error = new ApiError(
                ex.getStatus().value(),
                ex.getStatus().getReasonPhrase(),
                ex.getMessage()
        );
        return ResponseEntity.status(ex.getStatus()).body(error);
    }

    // Excepții aruncate din PL/pgSQL prin RAISE EXCEPTION
    @ExceptionHandler(SQLException.class)
    public ResponseEntity<ApiError> handleSQLException(SQLException ex) {
        log.error("SQLException din DB: {}", ex.getMessage());
        String msg = ex.getMessage() != null ? ex.getMessage() : "Eroare bază de date";

        // Excepția 1: NO_ACTIVE_SUBSCRIPTION (trigger pe consultations)
        if (msg.contains("NO_ACTIVE_SUBSCRIPTION")) {
            String detail = msg.contains(":") ? msg.split(":", 2)[1].trim() : msg;
            ApiError error = new ApiError(403, "Forbidden", detail);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }

        // Excepția 2: GUARDIAN_ONLY_FOR_CHILD (trigger pe guardians)
        if (msg.contains("GUARDIAN_ONLY_FOR_CHILD")) {
            String detail = msg.contains(":") ? msg.split(":", 2)[1].trim() : msg;
            ApiError error = new ApiError(400, "Bad Request", detail);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
        if (msg.contains("MAX_SYMPTOMS_REACHED")) {
            ApiError error = new ApiError(400, "Bad Request", "Maxim 3 simptome permise per consultație");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
        if (msg.contains("INVALID_STATUS_TRANSITION")) {
            ApiError error = new ApiError(409, "Conflict", "Tranziție de status invalidă");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }
        if (msg.contains("NO_SLOTS_AVAILABLE")) {
            ApiError error = new ApiError(409, "Conflict", "Nu există slot liber în următoarele 7 zile");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }
        if (msg.contains("EMERGENCY_NO_APPOINTMENT")) {
            ApiError error = new ApiError(400, "Bad Request", "Cazurile de urgență sunt redirecționate automat, nu se programează");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        ApiError error = new ApiError(500, "Database Error", "Eroare internă la baza de date");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    // DataIntegrityViolation — FK constraint, UNIQUE constraint etc.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("DataIntegrityViolation: {}", ex.getMessage());
        String msg = ex.getMessage() != null ? ex.getMessage() : "";

        if (msg.contains("users_email_key") || msg.contains("duplicate key") && msg.contains("email")) {
            ApiError error = new ApiError(409, "Conflict", "Există deja un cont cu această adresă de email");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }
        if (msg.contains("patients_cnp_key")) {
            ApiError error = new ApiError(409, "Conflict", "CNP-ul este deja înregistrat în sistem");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }

        // Prinde excepțiile PL/pgSQL care ajung ca DataIntegrityViolation
        Throwable cause = ex.getCause();
        if (cause != null && cause.getMessage() != null) {
            String causeMsg = cause.getMessage();
            if (causeMsg.contains("NO_ACTIVE_SUBSCRIPTION")) {
                String detail = causeMsg.contains(":") ? causeMsg.split(":", 2)[1].trim() : causeMsg;
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiError(403, "Forbidden", detail));
            }
            if (causeMsg.contains("GUARDIAN_ONLY_FOR_CHILD")) {
                String detail = causeMsg.contains(":") ? causeMsg.split(":", 2)[1].trim() : causeMsg;
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiError(400, "Bad Request", detail));
            }
        }

        ApiError error = new ApiError(409, "Conflict", "Constrângere de integritate violată");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // Validare
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        String firstError = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .findFirst()
                .orElse("Date invalide");
        ApiError error = new ApiError(400, "Bad Request", firstError);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // Login greșit
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex) {
        ApiError error = new ApiError(401, "Unauthorized", "Email sau parolă incorectă");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    // Acces interzis
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiError> handleAuthorizationDenied(AuthorizationDeniedException ex) {
        ApiError error = new ApiError(403, "Forbidden", "Nu ai permisiunea pentru această acțiune");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneral(Exception ex) {
        log.error("Eroare neașteptată: ", ex);
        ApiError error = new ApiError(500, "Internal Server Error", "A apărut o eroare neașteptată");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}