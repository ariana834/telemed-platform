package com.telemedicina.consultation.repository;

import com.telemedicina.consultation.model.*;

import java.util.List;
import java.util.Optional;

public interface ConsultationRepository {

    //consultatie
    Long create(Long patientId, String notes);
    Optional<Consultation> findById(Long id);
    List<Consultation> findAllByPatientId(Long patientId);

    //simptome
    void addSymptom(Long consultationId, String symptomName, String severity, int orderIndex);
    int countSymptoms(Long consultationId);
    List<ConsultationSymptom> findSymptoms(Long consultationId);

    //fisa medicala
    List<MedicalFormQuestion> findFormQuestions(Long consultationId);
    void saveAnswer(Long questionId, Long consultationId, String answerText);

    //diagnostice
    List<Diagnosis> findDiagnoses(Long consultationId);

    //proceduri stocate
    void computeDiagnosis(Long consultationId);
    // schedule_next_appointment
    Long scheduleAppointment(Long consultationId);
    // auto_generate_prescription
    Long generatePrescription(Long consultationId);
}