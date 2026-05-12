package com.telemedicina.consultation.service;

import com.telemedicina.consultation.dto.request.FormAnswersRequest;
import com.telemedicina.consultation.dto.request.SymptomRequest;
import com.telemedicina.consultation.dto.response.ConsultationDetailResponse;
import com.telemedicina.consultation.dto.response.ConsultationResponse;
import com.telemedicina.consultation.dto.response.MedicalFormResponse;
import com.telemedicina.consultation.mapper.ConsultationMapper;
import com.telemedicina.consultation.model.ComplexityLevel;
import com.telemedicina.consultation.model.Consultation;
import com.telemedicina.consultation.model.ConsultationStatus;
import com.telemedicina.consultation.model.ConsultationSymptom;
import com.telemedicina.consultation.model.Diagnosis;
import com.telemedicina.consultation.model.MedicalFormQuestion;
import com.telemedicina.consultation.repository.ConsultationRepository;
import com.telemedicina.patient.repository.PatientRepository;
import com.telemedicina.shared.exception.ApiException;
import com.telemedicina.shared.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultationServiceImpl implements ConsultationService {

    private final ConsultationRepository consultationRepo;
    private final PatientRepository patientRepo;
    private final ConsultationMapper mapper;

    @Override
    @Transactional
    public ConsultationResponse createConsultation(Long userId, String notes) {
        var patient = patientRepo.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Pacient", userId));

        // DB-ul verifica abonamentul activ prin trigger - daca nu are, arunca NoActiveSubscriptionException
        Long id = consultationRepo.create(patient.getId(), notes);

        var consultation = consultationRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Consultatie", id));

        ConsultationResponse response = mapper.toResponse(consultation);
        response.setSymptomCount(0);
        return response;
    }

    @Override
    public ConsultationDetailResponse getConsultation(Long consultationId, Long userId) {
        var c = getAndVerifyOwnership(consultationId, userId);
        return buildDetailResponse(c);
    }

    @Override
    public List<ConsultationResponse> getMyConsultations(Long userId) {
        var patient = patientRepo.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Pacient", userId));

        return consultationRepo.findAllByPatientId(patient.getId()).stream()
                .map(c -> {
                    ConsultationResponse r = mapper.toResponse(c);
                    r.setSymptomCount(consultationRepo.countSymptoms(c.getId()));
                    return r;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ConsultationResponse addSymptom(Long consultationId, Long userId, SymptomRequest request) {
        var c = getAndVerifyOwnership(consultationId, userId);

        if (c.getStatus() != ConsultationStatus.PENDING_FORM) {
            throw new ApiException("Nu mai poti adauga simptome - fisa a fost deja generata.", HttpStatus.BAD_REQUEST);
        }

        int currentCount = consultationRepo.countSymptoms(consultationId);
        if (currentCount >= 3) {
            throw new ApiException("Ai adaugat deja 3 simptome pentru aceasta consultatie.", HttpStatus.BAD_REQUEST);
        }

        // order_index = pozitia urmatoare (1, 2 sau 3)
        consultationRepo.addSymptom(
                consultationId,
                request.getSymptomName(),
                request.getSeverity(),
                currentCount + 1
        );

        // refetch pentru a vedea statusul actualizat de triggerele din DB
        var updated = consultationRepo.findById(consultationId)
                .orElseThrow(() -> new ResourceNotFoundException("Consultatie", consultationId));

        ConsultationResponse response = mapper.toResponse(updated);
        response.setSymptomCount(currentCount + 1);

        if (updated.getStatus() == ConsultationStatus.EMERGENCY_REDIRECT) {
            log.warn("Consultatie {} - redirectionata catre urgente!", consultationId);
        }

        return response;
    }

    @Override
    public List<MedicalFormResponse> getForm(Long consultationId, Long userId) {
        var c = getAndVerifyOwnership(consultationId, userId);

        if (c.getStatus() == ConsultationStatus.PENDING_FORM) {
            throw new ApiException("Fisa nu a fost inca generata. Adauga mai intai simptomele.", HttpStatus.BAD_REQUEST);
        }
        if (c.getStatus() == ConsultationStatus.EMERGENCY_REDIRECT) {
            throw new ApiException("Consultatia a fost redirectionata catre urgente. Nu exista fisa de completat.", HttpStatus.BAD_REQUEST);
        }

        return consultationRepo.findFormQuestions(consultationId).stream()
                .map(mapper::toFormResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ConsultationDetailResponse submitAnswers(Long consultationId, Long userId, FormAnswersRequest request) {
        var c = getAndVerifyOwnership(consultationId, userId);

        if (c.getStatus() != ConsultationStatus.FORM_GENERATED) {
            throw new ApiException("Fisa nu este activa. Status curent: " + c.getStatus(), HttpStatus.BAD_REQUEST);
        }

        for (FormAnswersRequest.AnswerItem answer : request.getAnswers()) {
            consultationRepo.saveAnswer(answer.getQuestionId(), consultationId, answer.getAnswerText());
        }

        // returnam starea curenta - clientul apeleaza /diagnose separat
        return buildDetailResponse(
                consultationRepo.findById(consultationId)
                        .orElseThrow(() -> new ResourceNotFoundException("Consultatie", consultationId))
        );
    }
    @Override
    @Transactional
    public ConsultationDetailResponse computeDiagnosis(Long consultationId, Long userId) {
        var c = getAndVerifyOwnership(consultationId, userId);

        if (c.getStatus() != ConsultationStatus.FORM_COMPLETED) {
            throw new ApiException("Fisa nu este completa inca.", HttpStatus.BAD_REQUEST);
        }

        consultationRepo.computeDiagnosis(consultationId);

        return buildDetailResponse(
                consultationRepo.findById(consultationId)
                        .orElseThrow(() -> new ResourceNotFoundException("Consultatie", consultationId))
        );
    }

    @Override
    @Transactional
    public Long scheduleAppointment(Long consultationId, Long userId) {
        var c = getAndVerifyOwnership(consultationId, userId);

        validateReadyForDecision(c);

        if (c.getComplexityLevel() == ComplexityLevel.SIMPLE) {
            throw new ApiException(
                    "Cazul tau este simplu si poate fi rezolvat cu reteta automata. Foloseste optiunea /prescribe.",
                    HttpStatus.BAD_REQUEST
            );
        }
        if (c.getComplexityLevel() == ComplexityLevel.EMERGENCY) {
            throw new ApiException(
                    "Cazurile de urgenta nu se programeaza online. Mergi la cel mai apropiat UPU.",
                    HttpStatus.BAD_REQUEST
            );
        }

        // DB-ul cauta primul slot liber in 7 zile, tinand cont de toti doctorii disponibili
        return consultationRepo.scheduleAppointment(consultationId);
    }

    @Override
    @Transactional
    public Long generatePrescription(Long consultationId, Long userId) {
        var c = getAndVerifyOwnership(consultationId, userId);

        validateReadyForDecision(c);

        if (c.getComplexityLevel() != ComplexityLevel.SIMPLE) {
            throw new ApiException(
                    "Reteta automata este disponibila doar pentru cazuri simple. Complexitate: " + c.getComplexityLevel(),
                    HttpStatus.BAD_REQUEST
            );
        }

        // DB-ul genereaza reteta OTC (fara antibiotice) pentru diagnostice din lista predefinita
        return consultationRepo.generatePrescription(consultationId);
    }

    // gaseste consultatia si verifica ca apartine utilizatorului curent
    private Consultation getAndVerifyOwnership(Long consultationId, Long userId) {
        var patient = patientRepo.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Pacient", userId));

        var c = consultationRepo.findById(consultationId)
                .orElseThrow(() -> new ResourceNotFoundException("Consultatie", consultationId));

        if (!c.getPatientId().equals(patient.getId())) {
            throw new ApiException("Nu ai acces la aceasta consultatie.", HttpStatus.FORBIDDEN);
        }

        return c;
    }

    // verifica ca suntem in starea potrivita pentru a lua o decizie (programare / reteta)
    private void validateReadyForDecision(Consultation c) {
        if (c.getStatus() != ConsultationStatus.DIAGNOSIS_PENDING) {
            throw new ApiException(
                    "Diagnosticul nu a fost inca calculat sau consultatia nu este in starea corecta. Status curent: " + c.getStatus(),
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    // construieste raspunsul complet cu toate datele asociate consultatiei
    private ConsultationDetailResponse buildDetailResponse(Consultation c) {
        List<ConsultationSymptom> symptoms = consultationRepo.findSymptoms(c.getId());
        List<MedicalFormQuestion> questions = consultationRepo.findFormQuestions(c.getId());
        List<Diagnosis> diagnoses = consultationRepo.findDiagnoses(c.getId());
        return mapper.toDetailResponse(c, symptoms, questions, diagnoses);
    }
}