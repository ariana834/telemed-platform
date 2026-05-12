package com.telemedicina.consultation.model;

/**
 * Nivelul de complexitate determinat de generate_medical_form().
 * Influenteaza durata consultatiei si ce optiuni are pacientul dupa diagnostic:
 * - SIMPLE  -> reteta automata (fara doctor)
 * - MEDIUM  -> consultatie 20 min
 * - COMPLEX -> consultatie 30 min
 * - EMERGENCY -> redirectionat catre UPU, nu se programeaza online
 */
public enum ComplexityLevel {
    SIMPLE,
    MEDIUM,
    COMPLEX,
    EMERGENCY
}