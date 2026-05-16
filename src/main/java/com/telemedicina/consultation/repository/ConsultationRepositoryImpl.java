package com.telemedicina.consultation.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemedicina.consultation.model.*;
import com.telemedicina.shared.exception.ApiException;
import com.telemedicina.shared.exception.AppointmentUnavailableException;
import com.telemedicina.shared.exception.NoActiveSubscriptionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ConsultationRepositoryImpl implements ConsultationRepository {

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper; // pentru parsarea JSONB -> List<String>

    //consultatie
    @Override
    public Long create(Long patientId, String notes) {
        // triggerul trg_check_active_subscription arunca NO_ACTIVE_SUBSCRIPTION daca nu are abonament
        try {
            return jdbc.queryForObject(
                    "INSERT INTO consultations (patient_id, notes) VALUES (?, ?) RETURNING id",
                    Long.class, patientId, notes
            );
        } catch (DataAccessException ex) {
            handleDbException(ex);
            throw ex;
        }
    }

    @Override
    public Optional<Consultation> findById(Long id) {
        List<Consultation> result = jdbc.query(
                "SELECT * FROM consultations WHERE id = ?",
                consultationRowMapper(), id
        );
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    @Override
    public List<Consultation> findAllByPatientId(Long patientId) {
        return jdbc.query(
                "SELECT * FROM consultations WHERE patient_id = ? ORDER BY created_at DESC",
                consultationRowMapper(), patientId
        );
    }

    @Override
    public void addSymptom(Long consultationId, String symptomName, String severity, int orderIndex) {
        // triggerul trg_check_max_symptoms arunca MAX_SYMPTOMS_REACHED dupa 3 simptome
        // triggerul trg_auto_generate_form genereaza automat fisa si trece la FORM_GENERATED
        try {
            jdbc.update(
                    "INSERT INTO consultation_symptoms (consultation_id, symptom_name, severity, order_index) " +
                            "VALUES (?, ?, ?, ?)",
                    consultationId, symptomName, severity, orderIndex
            );
        } catch (DataAccessException ex) {
            handleDbException(ex);
            throw ex;
        }
    }

    //programare
    @Override
    public int countSymptoms(Long consultationId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM consultation_symptoms WHERE consultation_id = ?",
                Integer.class, consultationId
        );
        return count != null ? count : 0;
    }

    @Override
    public List<ConsultationSymptom> findSymptoms(Long consultationId) {
        return jdbc.query(
                "SELECT * FROM consultation_symptoms WHERE consultation_id = ? ORDER BY order_index",
                symptomRowMapper(), consultationId
        );
    }


    //fisa medicala
    @Override
    public List<MedicalFormQuestion> findFormQuestions(Long consultationId) {
        return jdbc.query(
                "SELECT * FROM medical_form_questions WHERE consultation_id = ? ORDER BY order_index",
                questionRowMapper(), consultationId
        );
    }

    @Override
    public void saveAnswer(Long questionId, Long consultationId, String answerText) {
        // ON CONFLICT permite re-trimiterea unui raspuns (update la cel existent)
        // triggerul trg_check_form_completion seteaza FORM_COMPLETED cand toate intrebarile obligatorii au raspuns
        jdbc.update(
                "INSERT INTO medical_form_answers (question_id, consultation_id, answer_text) VALUES (?, ?, ?) " +
                        "ON CONFLICT (question_id, consultation_id) DO UPDATE SET answer_text = EXCLUDED.answer_text",
                questionId, consultationId, answerText
        );
    }

    @Override
    public List<Diagnosis> findDiagnoses(Long consultationId) {
        return jdbc.query(
                "SELECT * FROM diagnoses WHERE consultation_id = ? ORDER BY confidence_score DESC NULLS LAST",
                diagnosisRowMapper(), consultationId
        );
    }

    @Override
    public void computeDiagnosis(Long consultationId) {
        try {
            jdbc.execute(
                    "SELECT compute_preliminary_diagnosis(?)",
                    (PreparedStatementCallback<Void>) ps -> {
                        ps.setLong(1, consultationId);
                        ps.execute();
                        return null;
                    }
            );
        } catch (DataAccessException ex) {
            handleDbException(ex);
            throw ex;
        }
    }

    @Override
    public Long scheduleAppointment(Long consultationId) {
        // schedule_next_appointment - algoritm de tip brute-force:
        // itereaza din 10 in 10 minute in urmatoarele 7 zile pana gaseste un slot liber
        // tine cont de programul fiecarui doctor si de programarile existente
        try {
            return jdbc.queryForObject(
                    "SELECT schedule_next_appointment(?)",
                    Long.class, consultationId
            );
        } catch (DataAccessException ex) {
            handleDbException(ex);
            throw ex;
        }
    }

    @Override
    public Long generatePrescription(Long consultationId) {
        // auto_generate_prescription - genereaza reteta OTC (fara antibiotice)
        // disponibila doar pentru: Viroza respiratorie, Gripa, Toxiinfectie, Gastroenterita
        try {
            return jdbc.queryForObject(
                    "SELECT auto_generate_prescription(?)",
                    Long.class, consultationId
            );
        } catch (DataAccessException ex) {
            handleDbException(ex);
            throw ex;
        }
    }

    private RowMapper<Consultation> consultationRowMapper() {
        return (rs, rowNum) -> Consultation.builder()
                .id(rs.getLong("id"))
                .patientId(rs.getLong("patient_id"))
                .status(ConsultationStatus.valueOf(rs.getString("status")))
                .complexityLevel(rs.getString("complexity_level") != null
                        ? ComplexityLevel.valueOf(rs.getString("complexity_level")) : null)
                .emergencyRedirect(rs.getBoolean("emergency_redirect"))
                .notes(rs.getString("notes"))
                .createdAt(rs.getTimestamp("created_at") != null
                        ? rs.getTimestamp("created_at").toInstant() : null)
                .updatedAt(rs.getTimestamp("updated_at") != null
                        ? rs.getTimestamp("updated_at").toInstant() : null)
                .build();
    }

    private RowMapper<ConsultationSymptom> symptomRowMapper() {
        return (rs, rowNum) -> ConsultationSymptom.builder()
                .id(rs.getLong("id"))
                .consultationId(rs.getLong("consultation_id"))
                .symptomName(rs.getString("symptom_name"))
                .severity(rs.getString("severity"))
                .orderIndex(rs.getInt("order_index"))
                .createdAt(rs.getTimestamp("created_at") != null
                        ? rs.getTimestamp("created_at").toInstant() : null)
                .build();
    }

    private RowMapper<MedicalFormQuestion> questionRowMapper() {
        return (rs, rowNum) -> {
            // options vine ca JSONB din postgres - il parsam manual cu Jackson
            List<String> options = null;
            String optionsJson = rs.getString("options");
            if (optionsJson != null) {
                try {
                    options = objectMapper.readValue(optionsJson, new TypeReference<>() {});
                } catch (Exception e) {
                    log.warn("Nu am putut parsa optiunile pentru intrebarea id={}", rs.getLong("id"));
                    options = Collections.emptyList();
                }
            }

            return MedicalFormQuestion.builder()
                    .id(rs.getLong("id"))
                    .consultationId(rs.getLong("consultation_id"))
                    .questionText(rs.getString("question_text"))
                    .questionType(QuestionType.valueOf(rs.getString("question_type")))
                    .options(options)
                    .orderIndex(rs.getInt("order_index"))
                    .isRequired(rs.getBoolean("is_required"))
                    .createdAt(rs.getTimestamp("created_at") != null
                            ? rs.getTimestamp("created_at").toInstant() : null)
                    .build();
        };
    }

    private RowMapper<Diagnosis> diagnosisRowMapper() {
        return (rs, rowNum) -> Diagnosis.builder()
                .id(rs.getLong("id"))
                .consultationId(rs.getLong("consultation_id"))
                .diagnosisName(rs.getString("diagnosis_name"))
                .diagnosisType(DiagnosisType.valueOf(rs.getString("diagnosis_type")))
                .complexityLevel(ComplexityLevel.valueOf(rs.getString("complexity_level")))
                .icdCode(rs.getString("icd_code"))
                .confidenceScore(rs.getObject("confidence_score") != null
                        ? rs.getInt("confidence_score") : null)
                .notes(rs.getString("notes"))
                .createdBy(rs.getObject("created_by") != null
                        ? rs.getLong("created_by") : null)
                .createdAt(rs.getTimestamp("created_at") != null
                        ? rs.getTimestamp("created_at").toInstant() : null)
                .build();
    }

    private void handleDbException(DataAccessException ex) {
        String msg = ex.getMostSpecificCause().getMessage();
        if (msg == null) return;

        if (msg.contains("NO_ACTIVE_SUBSCRIPTION")) {
            // extragem patient_id din mesajul de eroare aruncat de trigger
            // formatul e: "NO_ACTIVE_SUBSCRIPTION: Pacientul 5 nu are un abonament activ"
            Long patientId = extractIdFromMessage(msg);
            throw new NoActiveSubscriptionException(patientId);
        }
        if (msg.contains("MAX_SYMPTOMS_REACHED")) {
            throw new ApiException("Ai atins limita de 3 simptome per consultatie.", HttpStatus.BAD_REQUEST);
        }
        if (msg.contains("SYMPTOMS_LOCKED")) {
            throw new ApiException("Nu mai poti adauga simptome - fisa medicala a fost deja generata.", HttpStatus.BAD_REQUEST);
        }
        if (msg.contains("INVALID_STATUS_TRANSITION")) {
            throw new ApiException("Actiunea nu este permisa in starea curenta a consultatiei.", HttpStatus.BAD_REQUEST);
        }
        if (msg.contains("NO_SLOTS_AVAILABLE")) {
            throw new AppointmentUnavailableException(
                    "Nu exista niciun slot disponibil in urmatoarele 7 zile. Incearca din nou mai tarziu."
            );
        }
        if (msg.contains("EMERGENCY_NO_APPOINTMENT")) {
            throw new ApiException(
                    "Cazurile de urgenta nu se programeaza online. Mergi la cel mai apropiat UPU.",
                    HttpStatus.BAD_REQUEST
            );
        }
        if (msg.contains("AUTO_PRESCRIPTION_NOT_ALLOWED")) {
            throw new ApiException(
                    "Reteta automata nu este disponibila pentru cazuri complexe. Programeaza-te la un doctor.",
                    HttpStatus.BAD_REQUEST
            );
        }
        if (msg.contains("DIAGNOSIS_NOT_ELIGIBLE")) {
            throw new ApiException(
                    "Diagnosticul tau necesita consultatie cu un medic. Foloseste optiunea de programare.",
                    HttpStatus.BAD_REQUEST
            );
        }
        if (msg.contains("DIAGNOSIS_ERROR") || msg.contains("FORM_GENERATION_ERROR")) {
            throw new ApiException("Eroare interna la procesarea consultatiei. Incearca din nou.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Long extractIdFromMessage(String msg) {
        try {
            for (String part : msg.split("\\s+")) {
                if (part.matches("\\d+")) {
                    return Long.parseLong(part);
                }
            }
        } catch (Exception ignored) {}
        return -1L;
    }
}