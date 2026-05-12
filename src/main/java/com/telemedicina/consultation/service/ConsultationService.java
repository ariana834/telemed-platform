package com.telemedicina.consultation.service;

import com.telemedicina.consultation.dto.request.FormAnswersRequest;
import com.telemedicina.consultation.dto.request.SymptomRequest;
import com.telemedicina.consultation.dto.response.ConsultationDetailResponse;
import com.telemedicina.consultation.dto.response.ConsultationResponse;
import com.telemedicina.consultation.dto.response.MedicalFormResponse;

import java.util.List;

public interface ConsultationService {

    // creaza o consultatie noua - DB verifica automat abonamentul activ
    ConsultationResponse createConsultation(Long userId, String notes);

    // detalii complete despre o consultatie (simptome + fisa + diagnostice)
    ConsultationDetailResponse getConsultation(Long consultationId, Long userId);

    // istoricul consultatiilor pacientului curent
    List<ConsultationResponse> getMyConsultations(Long userId);

    // adauga un simptom (max 3); dupa ultimul, DB genereaza automat fisa
    ConsultationResponse addSymptom(Long consultationId, Long userId, SymptomRequest request);

    // preia intrebarile generate de DB pentru aceasta consultatie
    List<MedicalFormResponse> getForm(Long consultationId, Long userId);

    // trimite raspunsurile la fisa; daca sunt complete, calculeaza automat diagnosticul
    ConsultationDetailResponse submitAnswers(Long consultationId, Long userId, FormAnswersRequest request);
    ConsultationDetailResponse computeDiagnosis(Long consultationId, Long userId);

    // programeaza la doctor - disponibil pentru medium si complex
    Long scheduleAppointment(Long consultationId, Long userId);

    // reteta automata OTC - disponibila doar pentru simple
    Long generatePrescription(Long consultationId, Long userId);
}