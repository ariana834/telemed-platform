package com.telemedicina.consultation.model;

/*
 De unde vine diagnosticul:
- AUTO_GENERATED -> calculat de compute_preliminary_diagnosis(), max 2 per consultatie
 - PRELIMINARY -> validat partial de sistem
 - CONFIRMED-> confirmat sau modificat de doctor, max 1 per consultatie
 */
public enum DiagnosisType {
    AUTO_GENERATED,
    PRELIMINARY,
    CONFIRMED
}