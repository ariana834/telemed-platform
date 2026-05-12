package com.telemedicina.consultation.model;

/**
 * State machine-ul consultatiei.
 * Tranzitiile valide sunt validate de triggerul trg_validate_status_transition din DB.
 * Nu poti sari pasi sau merge inapoi - orice tentativa arunca INVALID_STATUS_TRANSITION.
 */
public enum ConsultationStatus {
    PENDING_FORM,        // tocmai creata, asteapta simptomele
    FORM_GENERATED,      // fisa generata de DB dupa simptome, asteapta raspunsuri
    FORM_COMPLETED,      // toate intrebarile obligatorii au primit raspuns
    DIAGNOSIS_PENDING,   // diagnosticul a fost calculat, pacientul decide ce face
    SCHEDULED,           // programat la doctor
    IN_PROGRESS,         // consultatia cu doctorul e in desfasurare
    COMPLETED,           // finalizata
    CANCELLED,
    EMERGENCY_REDIRECT   // pacientul a fost redirectionat catre UPU
}