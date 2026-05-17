-- ================================================================
-- V8__stored_procedures.sql
-- 1. generate_medical_form
-- 2. compute_preliminary_diagnosis
-- 3. schedule_next_appointment  ← specialty-matching refacut
-- 4. auto_generate_prescription
-- ================================================================

CREATE OR REPLACE FUNCTION generate_medical_form(p_consultation_id BIGINT)
RETURNS VOID AS $$
DECLARE
v_patient_id          BIGINT;
    v_age_category        age_category;
    v_is_child            BOOLEAN;
    v_symptoms_text       TEXT;
    v_abdominal_severity  VARCHAR(20);
    v_order_idx           SMALLINT := 1;

    v_has_fever           BOOLEAN := FALSE;
    v_has_abdominal       BOOLEAN := FALSE;
    v_has_headache        BOOLEAN := FALSE;
    v_has_vomiting        BOOLEAN := FALSE;
    v_has_no_appetite     BOOLEAN := FALSE;
    v_has_cough           BOOLEAN := FALSE;
    v_has_chest_pain      BOOLEAN := FALSE;
    v_has_breathing       BOOLEAN := FALSE;
    v_has_fatigue         BOOLEAN := FALSE;
    v_has_rash            BOOLEAN := FALSE;
    v_has_sore_throat     BOOLEAN := FALSE;
    v_has_ear_pain        BOOLEAN := FALSE;

    v_has_diabetes        BOOLEAN := FALSE;
    v_has_hypertension    BOOLEAN := FALSE;
    v_has_heart_disease   BOOLEAN := FALSE;
    v_has_digestive       BOOLEAN := FALSE;
    v_has_allergies       BOOLEAN := FALSE;
BEGIN
SELECT c.patient_id, p.age_category
INTO   v_patient_id, v_age_category
FROM   consultations c
           JOIN   patients p ON p.id = c.patient_id
WHERE  c.id = p_consultation_id;

v_is_child := (v_age_category = 'CHILD');

SELECT string_agg(LOWER(symptom_name), ' ')
INTO   v_symptoms_text
FROM   consultation_symptoms
WHERE  consultation_id = p_consultation_id;

v_has_fever       := v_symptoms_text ILIKE '%fever%'      OR v_symptoms_text ILIKE '%temperature%'        OR v_symptoms_text ILIKE '%high temp%';
    v_has_abdominal   := v_symptoms_text ILIKE '%abdomin%'    OR v_symptoms_text ILIKE '%stomach%'            OR v_symptoms_text ILIKE '%belly%'       OR v_symptoms_text ILIKE '%abdomen%';
    v_has_headache    := v_symptoms_text ILIKE '%headache%'   OR v_symptoms_text ILIKE '%head pain%'          OR v_symptoms_text ILIKE '%migraine%';
    v_has_vomiting    := v_symptoms_text ILIKE '%vomit%'      OR v_symptoms_text ILIKE '%nausea%'             OR v_symptoms_text ILIKE '%throwing up%';
    v_has_no_appetite := v_symptoms_text ILIKE '%appetite%'   OR v_symptoms_text ILIKE '%no appetite%'        OR v_symptoms_text ILIKE '%not eating%';
    v_has_cough       := v_symptoms_text ILIKE '%cough%'      OR v_symptoms_text ILIKE '%coughing%';
    v_has_chest_pain  := v_symptoms_text ILIKE '%chest%'      OR v_symptoms_text ILIKE '%chest pain%'         OR v_symptoms_text ILIKE '%chest tightness%';
    v_has_breathing   := v_symptoms_text ILIKE '%breath%'     OR v_symptoms_text ILIKE '%shortness of breath%' OR v_symptoms_text ILIKE '%dyspnea%'   OR v_symptoms_text ILIKE '%breathing%';
    v_has_fatigue     := v_symptoms_text ILIKE '%fatigue%'    OR v_symptoms_text ILIKE '%tired%'              OR v_symptoms_text ILIKE '%exhausted%'   OR v_symptoms_text ILIKE '%weakness%';
    v_has_rash        := v_symptoms_text ILIKE '%rash%'       OR v_symptoms_text ILIKE '%spots%'              OR v_symptoms_text ILIKE '%hives%'        OR v_symptoms_text ILIKE '%skin redness%';
    v_has_sore_throat := v_symptoms_text ILIKE '%throat%'     OR v_symptoms_text ILIKE '%sore throat%'        OR v_symptoms_text ILIKE '%swallowing%';
    v_has_ear_pain    := v_symptoms_text ILIKE '%ear%'        OR v_symptoms_text ILIKE '%ear pain%'           OR v_symptoms_text ILIKE '%earache%';

SELECT severity INTO v_abdominal_severity
FROM   consultation_symptoms
WHERE  consultation_id = p_consultation_id
  AND  (LOWER(symptom_name) ILIKE '%abdomin%' OR LOWER(symptom_name) ILIKE '%stomach%' OR LOWER(symptom_name) ILIKE '%belly%')
    LIMIT 1;

SELECT
    BOOL_OR(LOWER(condition_name) ILIKE '%diabet%'),
    BOOL_OR(LOWER(condition_name) ILIKE '%hypertension%' OR LOWER(condition_name) ILIKE '%high blood pressure%'),
    BOOL_OR(LOWER(condition_name) ILIKE '%heart%'        OR LOWER(condition_name) ILIKE '%cardiac%'),
    BOOL_OR(LOWER(condition_name) ILIKE '%digestive%'    OR LOWER(condition_name) ILIKE '%gastritis%' OR LOWER(condition_name) ILIKE '%gastric%'),
    BOOL_OR(LOWER(condition_name) ILIKE '%allerg%')
INTO v_has_diabetes, v_has_hypertension, v_has_heart_disease, v_has_digestive, v_has_allergies
FROM chronic_conditions
WHERE patient_id = v_patient_id AND is_active = TRUE;

-- RULE 1: EMERGENCY — severe abdominal pain + vomiting
IF v_has_abdominal AND v_has_vomiting AND (v_abdominal_severity = 'SEVERE' OR NOT v_has_fever) THEN
UPDATE consultations SET status = 'EMERGENCY_REDIRECT', complexity_level = 'EMERGENCY', emergency_redirect = TRUE WHERE id = p_consultation_id;
INSERT INTO medical_form_questions (consultation_id, question_text, question_type, options, order_index, is_required) VALUES
                                                                                                                          (p_consultation_id, 'Is the pain localized in the lower right side of the abdomen?', 'YES_NO', NULL, 1, TRUE),
                                                                                                                          (p_consultation_id, 'How long have the symptoms been present?', 'MULTIPLE_CHOICE', '["Less than 1 hour", "1–6 hours", "6–12 hours", "More than 12 hours"]', 2, TRUE),
                                                                                                                          (p_consultation_id, 'Has the pain progressively worsened since it started?', 'YES_NO', NULL, 3, TRUE),
                                                                                                                          (p_consultation_id, 'Do you have a fever?', 'YES_NO', NULL, 4, TRUE);
RAISE NOTICE 'EMERGENCY_REDIRECT: Consultation % redirected to emergency', p_consultation_id;
        RETURN;
END IF;

    -- COMMON questions
INSERT INTO medical_form_questions (consultation_id, question_text, question_type, options, order_index, is_required) VALUES
    (p_consultation_id, 'How many days have you had these symptoms?', 'MULTIPLE_CHOICE', '["1 day", "2–3 days", "4–7 days", "More than a week"]', v_order_idx, TRUE);
v_order_idx := v_order_idx + 1;

INSERT INTO medical_form_questions (consultation_id, question_text, question_type, options, order_index, is_required) VALUES
    (p_consultation_id, 'How would you rate the overall intensity of your symptoms?', 'MULTIPLE_CHOICE', '["Mild — I can carry out daily activities normally", "Moderate — activities are affected", "Severe — I cannot carry out daily activities"]', v_order_idx, TRUE);
v_order_idx := v_order_idx + 1;

    -- RULE 2: CARDIAC / PULMONARY
    IF v_has_chest_pain AND v_has_breathing THEN
        INSERT INTO medical_form_questions (consultation_id, question_text, question_type, options, order_index, is_required) VALUES
            (p_consultation_id, 'How would you describe the chest pain?', 'MULTIPLE_CHOICE', '["Dull / pressure-like", "Sharp / stabbing", "Burning", "Tightness"]', v_order_idx, TRUE),
            (p_consultation_id, 'Does the pain radiate to your arm, jaw, or back?', 'YES_NO', NULL, v_order_idx + 1, TRUE),
            (p_consultation_id, 'Does the shortness of breath occur at rest or only during exertion?', 'MULTIPLE_CHOICE', '["At rest", "During light exertion", "Only during intense exertion"]', v_order_idx + 2, TRUE),
            (p_consultation_id, 'Have you experienced palpitations, dizziness, or fainting?', 'CHECKBOX', '["Palpitations", "Dizziness", "Fainting", "None of the above"]', v_order_idx + 3, TRUE),
            (p_consultation_id, 'Have you had a recent respiratory infection (cold, flu, COVID-19)?', 'YES_NO', NULL, v_order_idx + 4, FALSE);
        v_order_idx := v_order_idx + 5;
        IF v_has_heart_disease OR v_has_hypertension THEN
            INSERT INTO medical_form_questions (consultation_id, question_text, question_type, options, order_index, is_required) VALUES
                (p_consultation_id, 'What was your last recorded blood pressure reading?', 'OPEN_TEXT', NULL, v_order_idx, TRUE),
                (p_consultation_id, 'Are you currently taking your prescribed cardiac / blood pressure medication?', 'YES_NO', NULL, v_order_idx + 1, TRUE);
            v_order_idx := v_order_idx + 2;
END IF;
UPDATE consultations SET complexity_level = 'COMPLEX' WHERE id = p_consultation_id;
RETURN;
END IF;

    -- RULE 3: GASTROINTESTINAL
    IF v_has_fever AND v_has_abdominal AND v_has_vomiting THEN
        INSERT INTO medical_form_questions (consultation_id, question_text, question_type, options, order_index, is_required) VALUES
            (p_consultation_id, 'What is your current temperature?', 'MULTIPLE_CHOICE', '["37–37.5 °C (low-grade)", "37.5–38.5 °C (moderate)", "38.5–39.5 °C (high)", "Above 39.5 °C"]', v_order_idx, TRUE),
            (p_consultation_id, 'Did you consume potentially spoiled food in the last 24 hours?', 'YES_NO', NULL, v_order_idx + 1, TRUE),
            (p_consultation_id, 'Do you have diarrhoea?', 'YES_NO', NULL, v_order_idx + 2, TRUE),
            (p_consultation_id, 'Are other people around you experiencing the same symptoms?', 'YES_NO', NULL, v_order_idx + 3, TRUE),
            (p_consultation_id, 'Is vomiting frequent (more than 3 episodes per day)?', 'YES_NO', NULL, v_order_idx + 4, FALSE);
        v_order_idx := v_order_idx + 5;
        IF v_has_digestive THEN
            INSERT INTO medical_form_questions (consultation_id, question_text, question_type, options, order_index, is_required) VALUES
                (p_consultation_id, 'Do the current symptoms resemble previous episodes of your chronic digestive condition?', 'YES_NO', NULL, v_order_idx, FALSE),
                (p_consultation_id, 'Are you currently following a treatment plan for your digestive condition?', 'YES_NO', NULL, v_order_idx + 1, TRUE);
            v_order_idx := v_order_idx + 2;
END IF;
UPDATE consultations SET complexity_level = 'MEDIUM' WHERE id = p_consultation_id;
RETURN;
END IF;

    -- RULE 4: UPPER RESPIRATORY
    IF v_has_fever AND v_has_headache AND (v_has_cough OR v_has_fatigue) THEN
        INSERT INTO medical_form_questions (consultation_id, question_text, question_type, options, order_index, is_required) VALUES
            (p_consultation_id, 'What is your current temperature?', 'MULTIPLE_CHOICE', '["37–37.5 °C (low-grade)", "37.5–38.5 °C (moderate)", "38.5–39.5 °C (high)", "Above 39.5 °C"]', v_order_idx, TRUE),
            (p_consultation_id, 'Do you have muscle or joint pain?', 'YES_NO', NULL, v_order_idx + 1, TRUE),
            (p_consultation_id, 'Do you have a sore throat or difficulty swallowing?', 'YES_NO', NULL, v_order_idx + 2, TRUE),
            (p_consultation_id, 'Which nasal symptoms are you experiencing?', 'CHECKBOX', '["Blocked nose", "Clear nasal discharge", "Yellow/green nasal discharge", "None"]', v_order_idx + 3, FALSE),
            (p_consultation_id, 'Have you been vaccinated against influenza this season?', 'YES_NO', NULL, v_order_idx + 4, FALSE);
        v_order_idx := v_order_idx + 5;
        IF v_has_hypertension THEN
            INSERT INTO medical_form_questions (consultation_id, question_text, question_type, options, order_index, is_required) VALUES
                (p_consultation_id, 'Have you measured your blood pressure recently? Please share the reading.', 'OPEN_TEXT', NULL, v_order_idx, TRUE);
            v_order_idx := v_order_idx + 1;
END IF;
UPDATE consultations SET complexity_level = 'SIMPLE' WHERE id = p_consultation_id;
RETURN;
END IF;

    -- RULE 5: PAEDIATRIC
    IF v_is_child AND v_has_fever AND (v_has_vomiting OR v_has_no_appetite) THEN
        INSERT INTO medical_form_questions (consultation_id, question_text, question_type, options, order_index, is_required) VALUES
            (p_consultation_id, 'Does the child have any skin rash or spots?', 'YES_NO', NULL, v_order_idx, TRUE),
            (p_consultation_id, 'Is the child pulling at their ears or complaining of ear pain?', 'YES_NO', NULL, v_order_idx + 1, TRUE),
            (p_consultation_id, 'What is the child''s current temperature?', 'MULTIPLE_CHOICE', '["37–37.5 °C", "37.5–38.5 °C", "38.5–39.5 °C", "Above 39.5 °C"]', v_order_idx + 2, TRUE),
            (p_consultation_id, 'Has the child been in contact with other ill children in the past 7 days?', 'YES_NO', NULL, v_order_idx + 3, TRUE),
            (p_consultation_id, 'Is the child''s vaccination schedule up to date?', 'YES_NO', NULL, v_order_idx + 4, FALSE),
            (p_consultation_id, 'Is the child having difficulty breathing or breathing faster than normal?', 'YES_NO', NULL, v_order_idx + 5, TRUE);
        v_order_idx := v_order_idx + 6;
UPDATE consultations SET complexity_level = 'MEDIUM' WHERE id = p_consultation_id;
RETURN;
END IF;

    -- DEFAULT
INSERT INTO medical_form_questions (consultation_id, question_text, question_type, options, order_index, is_required) VALUES
                                                                                                                          (p_consultation_id, 'Have you experienced similar symptoms in the past?', 'YES_NO', NULL, v_order_idx, FALSE),
                                                                                                                          (p_consultation_id, 'Are you currently taking any medication or supplements?', 'YES_NO', NULL, v_order_idx + 1, TRUE),
                                                                                                                          (p_consultation_id, 'Do you have any known drug allergies?', 'YES_NO', NULL, v_order_idx + 2, TRUE),
                                                                                                                          (p_consultation_id, 'Please briefly describe how your symptoms have evolved since they started.', 'OPEN_TEXT', NULL, v_order_idx + 3, TRUE);
v_order_idx := v_order_idx + 4;
    IF v_has_diabetes THEN
        INSERT INTO medical_form_questions (consultation_id, question_text, question_type, options, order_index, is_required) VALUES
            (p_consultation_id, 'Have you monitored your blood glucose in the last 24 hours? Please share the readings.', 'OPEN_TEXT', NULL, v_order_idx, TRUE);
        v_order_idx := v_order_idx + 1;
END IF;
    IF v_has_allergies THEN
        INSERT INTO medical_form_questions (consultation_id, question_text, question_type, options, order_index, is_required) VALUES
            (p_consultation_id, 'Have you recently been exposed to known allergens (dust, pollen, food, medication)?', 'YES_NO', NULL, v_order_idx, TRUE);
        v_order_idx := v_order_idx + 1;
END IF;
UPDATE consultations SET complexity_level = 'SIMPLE' WHERE id = p_consultation_id;

EXCEPTION WHEN OTHERS THEN
    RAISE EXCEPTION 'FORM_GENERATION_ERROR: Failed to generate form for consultation %: %', p_consultation_id, SQLERRM;
END;
$$ LANGUAGE plpgsql;


-- ================================================================
-- 2. COMPUTE PRELIMINARY DIAGNOSIS
-- ================================================================

CREATE OR REPLACE FUNCTION compute_preliminary_diagnosis(p_consultation_id BIGINT)
RETURNS VOID AS $$
DECLARE
v_patient_id            BIGINT;
    v_age_category          age_category;
    v_complexity            complexity_level;
    v_symptoms_text         TEXT;
    v_score_flu             INTEGER := 0;
    v_score_cold            INTEGER := 0;
    v_score_food_poisoning  INTEGER := 0;
    v_score_bronchitis      INTEGER := 0;
    v_score_otitis          INTEGER := 0;
    v_score_gastroenteritis INTEGER := 0;
    v_score_cardiac         INTEGER := 0;
    v_bad_food              BOOLEAN := FALSE;
    v_others_sick           BOOLEAN := FALSE;
    v_high_fever            BOOLEAN := FALSE;
    v_muscle_pain           BOOLEAN := FALSE;
    v_ear_pain              BOOLEAN := FALSE;
    v_has_rash              BOOLEAN := FALSE;
    v_has_diarrhea          BOOLEAN := FALSE;
    v_pain_radiates         BOOLEAN := FALSE;
    v_has_palpitations      BOOLEAN := FALSE;
    v_diagnosis1            VARCHAR(255);
    v_diagnosis2            VARCHAR(255);
    v_icd1                  VARCHAR(10);
    v_icd2                  VARCHAR(10);
    v_score1                INTEGER;
    v_score2                INTEGER;
BEGIN
SELECT c.patient_id, p.age_category, c.complexity_level
INTO   v_patient_id, v_age_category, v_complexity
FROM   consultations c JOIN patients p ON p.id = c.patient_id
WHERE  c.id = p_consultation_id;

IF v_complexity = 'EMERGENCY' THEN
        INSERT INTO diagnoses (consultation_id, diagnosis_name, diagnosis_type, complexity_level, icd_code, confidence_score, notes)
        VALUES (p_consultation_id, 'Possible Acute Appendicitis — requires emergency surgical evaluation', 'AUTO_GENERATED', 'EMERGENCY', 'K37', 75, 'Symptoms indicate a possible surgical emergency. Patient redirected to the nearest ER.');
        RETURN;
END IF;

SELECT string_agg(LOWER(symptom_name), ' ') INTO v_symptoms_text FROM consultation_symptoms WHERE consultation_id = p_consultation_id;

SELECT
    BOOL_OR(LOWER(a.answer_text) = 'yes' AND q.question_text ILIKE '%spoiled food%'),
    BOOL_OR(LOWER(a.answer_text) = 'yes' AND q.question_text ILIKE '%other people%'),
    BOOL_OR(a.answer_text ILIKE '%39%' OR a.answer_text ILIKE '%40%' OR a.answer_text ILIKE '%above 39%'),
    BOOL_OR(LOWER(a.answer_text) = 'yes' AND q.question_text ILIKE '%muscle%'),
    BOOL_OR(LOWER(a.answer_text) = 'yes' AND q.question_text ILIKE '%ear%'),
    BOOL_OR(LOWER(a.answer_text) = 'yes' AND q.question_text ILIKE '%rash%'),
    BOOL_OR(LOWER(a.answer_text) = 'yes' AND q.question_text ILIKE '%diarrh%'),
    BOOL_OR(LOWER(a.answer_text) = 'yes' AND q.question_text ILIKE '%radiat%'),
    BOOL_OR(a.answer_text ILIKE '%palpitation%')
INTO v_bad_food, v_others_sick, v_high_fever, v_muscle_pain, v_ear_pain, v_has_rash, v_has_diarrhea, v_pain_radiates, v_has_palpitations
FROM medical_form_answers a JOIN medical_form_questions q ON q.id = a.question_id
WHERE a.consultation_id = p_consultation_id;

IF v_symptoms_text ILIKE '%fever%'   THEN v_score_flu := v_score_flu + 15; END IF;
    IF v_high_fever                       THEN v_score_flu := v_score_flu + 20; END IF;
    IF v_muscle_pain                      THEN v_score_flu := v_score_flu + 25; END IF;
    IF v_symptoms_text ILIKE '%headache%' THEN v_score_flu := v_score_flu + 10; END IF;
    IF v_others_sick                      THEN v_score_flu := v_score_flu + 15; END IF;
    IF v_symptoms_text ILIKE '%fatigue%'  THEN v_score_flu := v_score_flu + 10; END IF;

    IF v_symptoms_text ILIKE '%fever%' AND NOT v_high_fever THEN v_score_cold := v_score_cold + 25; END IF;
    IF v_symptoms_text ILIKE '%cough%'   THEN v_score_cold := v_score_cold + 20; END IF;
    IF v_symptoms_text ILIKE '%nasal%' OR v_symptoms_text ILIKE '%runny%' THEN v_score_cold := v_score_cold + 15; END IF;
    IF v_symptoms_text ILIKE '%throat%'  THEN v_score_cold := v_score_cold + 10; END IF;

    IF v_bad_food                          THEN v_score_food_poisoning := v_score_food_poisoning + 35; END IF;
    IF v_symptoms_text ILIKE '%vomit%' OR v_symptoms_text ILIKE '%nausea%' THEN v_score_food_poisoning := v_score_food_poisoning + 20; END IF;
    IF v_has_diarrhea                      THEN v_score_food_poisoning := v_score_food_poisoning + 20; END IF;
    IF v_others_sick                       THEN v_score_food_poisoning := v_score_food_poisoning + 15; END IF;

    IF v_symptoms_text ILIKE '%stomach%' OR v_symptoms_text ILIKE '%abdom%' THEN v_score_gastroenteritis := v_score_gastroenteritis + 20; END IF;
    IF v_symptoms_text ILIKE '%vomit%'  OR v_symptoms_text ILIKE '%nausea%' THEN v_score_gastroenteritis := v_score_gastroenteritis + 20; END IF;
    IF v_symptoms_text ILIKE '%fever%' AND NOT v_high_fever THEN v_score_gastroenteritis := v_score_gastroenteritis + 15; END IF;
    IF v_has_diarrhea THEN v_score_gastroenteritis := v_score_gastroenteritis + 10; END IF;

    IF v_symptoms_text ILIKE '%cough%' THEN v_score_bronchitis := v_score_bronchitis + 30; END IF;
    IF v_symptoms_text ILIKE '%chest%' THEN v_score_bronchitis := v_score_bronchitis + 25; END IF;
    IF v_high_fever                     THEN v_score_bronchitis := v_score_bronchitis + 10; END IF;

    IF v_age_category = 'CHILD' THEN
        IF v_ear_pain                       THEN v_score_otitis := v_score_otitis + 45; END IF;
        IF v_symptoms_text ILIKE '%fever%'  THEN v_score_otitis := v_score_otitis + 20; END IF;
        IF v_has_rash                       THEN v_score_otitis := v_score_otitis - 15; END IF;
END IF;

    IF v_symptoms_text ILIKE '%chest%'   THEN v_score_cardiac := v_score_cardiac + 30; END IF;
    IF v_symptoms_text ILIKE '%breath%'  THEN v_score_cardiac := v_score_cardiac + 30; END IF;
    IF v_pain_radiates                    THEN v_score_cardiac := v_score_cardiac + 25; END IF;
    IF v_has_palpitations                 THEN v_score_cardiac := v_score_cardiac + 20; END IF;
    IF v_symptoms_text ILIKE '%fatigue%' THEN v_score_cardiac := v_score_cardiac + 10; END IF;

    CREATE TEMP TABLE IF NOT EXISTS _temp_diag_scores (diag_name VARCHAR(255), icd VARCHAR(10), score INTEGER) ON COMMIT DROP;
DELETE FROM _temp_diag_scores;
INSERT INTO _temp_diag_scores VALUES
                                  ('Influenza (Flu)',                          'J10', v_score_flu),
                                  ('Upper Respiratory Tract Infection (Cold)', 'J06', v_score_cold),
                                  ('Food Poisoning',                           'A05', v_score_food_poisoning),
                                  ('Acute Gastroenteritis',                    'A09', v_score_gastroenteritis),
                                  ('Acute Bronchitis',                         'J20', v_score_bronchitis),
                                  ('Acute Otitis Media',                       'H66', v_score_otitis),
                                  ('Possible Cardiac / Pulmonary Condition',   'I51', v_score_cardiac);

SELECT diag_name, icd, score INTO v_diagnosis1, v_icd1, v_score1 FROM _temp_diag_scores WHERE score > 20 ORDER BY score DESC LIMIT 1;
SELECT diag_name, icd, score INTO v_diagnosis2, v_icd2, v_score2 FROM _temp_diag_scores WHERE score > 20 AND diag_name != COALESCE(v_diagnosis1,'') ORDER BY score DESC LIMIT 1;

IF v_diagnosis1 IS NOT NULL THEN
        INSERT INTO diagnoses (consultation_id, diagnosis_name, diagnosis_type, complexity_level, icd_code, confidence_score)
        VALUES (p_consultation_id, v_diagnosis1, 'AUTO_GENERATED', v_complexity, v_icd1, LEAST(90, 40 + v_score1 / 2));
END IF;
    IF v_diagnosis2 IS NOT NULL THEN
        INSERT INTO diagnoses (consultation_id, diagnosis_name, diagnosis_type, complexity_level, icd_code, confidence_score)
        VALUES (p_consultation_id, v_diagnosis2, 'AUTO_GENERATED', v_complexity, v_icd2, LEAST(70, 30 + v_score2 / 3));
END IF;
    IF v_diagnosis1 IS NULL THEN
        INSERT INTO diagnoses (consultation_id, diagnosis_name, diagnosis_type, complexity_level, confidence_score, notes)
        VALUES (p_consultation_id, 'Unspecified condition — medical evaluation required', 'AUTO_GENERATED', 'MEDIUM', 30, 'Symptoms do not match a clear diagnostic pattern. A consultation with a doctor is recommended.');
END IF;

UPDATE consultations SET status = 'DIAGNOSIS_PENDING' WHERE id = p_consultation_id;

EXCEPTION WHEN OTHERS THEN
    RAISE EXCEPTION 'DIAGNOSIS_ERROR: Failed to compute diagnosis for consultation %: %', p_consultation_id, SQLERRM;
END;
$$ LANGUAGE plpgsql;


-- ================================================================
-- 3. SCHEDULE NEXT APPOINTMENT — cu specialty matching
-- ================================================================

CREATE OR REPLACE FUNCTION schedule_next_appointment(p_consultation_id BIGINT)
RETURNS BIGINT AS $$
DECLARE
v_patient_id             BIGINT;
    v_age_category           age_category;
    v_complexity             complexity_level;
    v_symptoms_text          TEXT;
    v_top_diagnosis          TEXT;
    v_duration               SMALLINT;
    v_search_from            TIMESTAMPTZ;
    v_search_limit           TIMESTAMPTZ;
    v_found                  BOOLEAN := FALSE;
    v_appointment_id         BIGINT;
    v_slot_start             TIMESTAMPTZ;
    v_slot_end               TIMESTAMPTZ;
    v_chosen_doctor          BIGINT;
    r_doctor                 RECORD;
    v_day_of_week            SMALLINT;
    v_preferred_specialties  TEXT[];
    v_fallback_specialties   TEXT[];
    v_current_specialties    TEXT[];
    v_pass                   INTEGER := 1;
BEGIN
SELECT c.patient_id, p.age_category, c.complexity_level
INTO   v_patient_id, v_age_category, v_complexity
FROM   consultations c
           JOIN   patients p ON p.id = c.patient_id
WHERE  c.id = p_consultation_id;

IF v_complexity = 'EMERGENCY' THEN
        RAISE EXCEPTION 'EMERGENCY_NO_APPOINTMENT: Emergency cases cannot be scheduled online — go to the nearest ER';
END IF;

    v_duration := CASE v_complexity
        WHEN 'SIMPLE'  THEN 10
        WHEN 'MEDIUM'  THEN 20
        WHEN 'COMPLEX' THEN 30
        ELSE 20
END;

    -- diagnosticul principal pentru specialty matching
SELECT LOWER(diagnosis_name) INTO v_top_diagnosis
FROM   diagnoses
WHERE  consultation_id = p_consultation_id
ORDER  BY confidence_score DESC LIMIT 1;

SELECT LOWER(string_agg(symptom_name, ' ')) INTO v_symptoms_text
FROM   consultation_symptoms WHERE consultation_id = p_consultation_id;

v_symptoms_text := COALESCE(v_symptoms_text, '');
    v_top_diagnosis := COALESCE(v_top_diagnosis, '');

    -- ── SPECIALTY MATCHING LOGIC ─────────────────────────────────
    IF v_age_category = 'CHILD' THEN
        -- Copii → intotdeauna pediatru, ORL pentru urechi
        IF v_symptoms_text ILIKE '%ear%' THEN
            v_preferred_specialties := ARRAY['ORL', 'Pediatrie'];
            v_fallback_specialties  := ARRAY['Medicina de Familie'];
ELSE
            v_preferred_specialties := ARRAY['Pediatrie'];
            v_fallback_specialties  := ARRAY['ORL', 'Medicina de Familie'];
END IF;

    ELSIF v_top_diagnosis ILIKE '%cardiac%'
       OR v_top_diagnosis ILIKE '%pulmonar%'
       OR v_top_diagnosis ILIKE '%heart%'
       OR v_symptoms_text ILIKE '%chest%'
       OR v_symptoms_text ILIKE '%breath%' THEN
        -- Cardiac / pulmonar → cardiolog sau pneumolog
        v_preferred_specialties := ARRAY['Cardiologie', 'Pneumologie'];
        v_fallback_specialties  := ARRAY['Medicina Interna'];

    ELSIF v_top_diagnosis ILIKE '%gastroenterit%'
       OR v_top_diagnosis ILIKE '%food poisoning%'
       OR v_symptoms_text ILIKE '%abdom%'
       OR v_symptoms_text ILIKE '%stomach%'
       OR v_symptoms_text ILIKE '%vomit%' THEN
        -- Digestiv → gastroenterolog
        v_preferred_specialties := ARRAY['Gastroenterologie', 'Medicina Interna'];
        v_fallback_specialties  := ARRAY['Medicina de Familie'];

    ELSIF v_top_diagnosis ILIKE '%otit%'
       OR v_symptoms_text ILIKE '%ear%'
       OR v_symptoms_text ILIKE '%throat%'
       OR v_symptoms_text ILIKE '%nasal%' THEN
        -- ORL — otita, gat, nas
        v_preferred_specialties := ARRAY['ORL', 'Medicina de Familie'];
        v_fallback_specialties  := ARRAY['Medicina Interna', 'Pediatrie'];

    ELSIF v_top_diagnosis ILIKE '%bronchit%'
       OR v_top_diagnosis ILIKE '%flu%'
       OR v_top_diagnosis ILIKE '%influenza%'
       OR v_top_diagnosis ILIKE '%cold%'
       OR v_symptoms_text ILIKE '%cough%' THEN
        -- Respirator / gripa → pneumolog sau medicina interna
        v_preferred_specialties := ARRAY['Pneumologie', 'Medicina Interna'];
        v_fallback_specialties  := ARRAY['Medicina de Familie'];

ELSE
        -- Default
        v_preferred_specialties := ARRAY['Medicina de Familie', 'Medicina Interna'];
        v_fallback_specialties  := ARRAY['Cardiologie', 'Pneumologie', 'Gastroenterologie', 'ORL', 'Pediatrie'];
END IF;

    -- ── CAUTARE SLOT — 2 pase: preferate → fallback ──────────────
    WHILE v_pass <= 2 LOOP
        v_current_specialties := CASE v_pass WHEN 1 THEN v_preferred_specialties ELSE v_fallback_specialties END;
        v_search_from  := DATE_TRUNC('hour', NOW()) + INTERVAL '1 hour';
        v_search_limit := v_search_from + INTERVAL '7 days';

        WHILE v_search_from < v_search_limit AND NOT v_found LOOP
            v_day_of_week := (EXTRACT(ISODOW FROM v_search_from)::SMALLINT - 1);

FOR r_doctor IN
SELECT d.id AS doctor_id
FROM   doctors d
           JOIN   doctor_schedules ds ON ds.doctor_id = d.id
WHERE  d.is_available     = TRUE
  AND  d.specialization   = ANY(v_current_specialties)
  AND  ds.day_of_week     = v_day_of_week
  AND  ds.is_active       = TRUE
  AND  ds.start_time     <= v_search_from::TIME
                  AND  ds.end_time       >= (v_search_from + (v_duration || ' minutes')::INTERVAL)::TIME
ORDER BY
    array_position(v_current_specialties, d.specialization),
    d.id
    LOOP
    IF NOT EXISTS (
    SELECT 1 FROM appointments
    WHERE  doctor_id = r_doctor.doctor_id
    AND  status NOT IN ('CANCELLED', 'NO_SHOW')
    AND  tstzrange(start_time, end_time) &&
    tstzrange(v_search_from, v_search_from + (v_duration || ' minutes')::INTERVAL)
    ) THEN
    v_slot_start    := v_search_from;
v_slot_end      := v_search_from + (v_duration || ' minutes')::INTERVAL;
                    v_chosen_doctor := r_doctor.doctor_id;
                    v_found         := TRUE;
                    EXIT;
END IF;
END LOOP;

            IF NOT v_found THEN
                v_search_from := v_search_from + INTERVAL '10 minutes';
END IF;
END LOOP;

        EXIT WHEN v_found;
        v_pass := v_pass + 1;
END LOOP;

    IF NOT v_found THEN
        RAISE EXCEPTION 'NO_SLOTS_AVAILABLE: No available slot in the next 7 days for complexity %', v_complexity;
END IF;

INSERT INTO appointments (consultation_id, doctor_id, patient_id, start_time, end_time, duration_minutes, status)
VALUES (p_consultation_id, v_chosen_doctor, v_patient_id, v_slot_start, v_slot_end, v_duration, 'SCHEDULED')
    RETURNING id INTO v_appointment_id;

UPDATE consultations SET status = 'SCHEDULED' WHERE id = p_consultation_id;
RETURN v_appointment_id;

EXCEPTION WHEN OTHERS THEN
    RAISE EXCEPTION 'SCHEDULING_ERROR: Failed to schedule consultation %: %', p_consultation_id, SQLERRM;
END;
$$ LANGUAGE plpgsql;


-- ================================================================
-- 4. AUTO GENERATE PRESCRIPTION
-- ================================================================

CREATE OR REPLACE FUNCTION auto_generate_prescription(p_consultation_id BIGINT)
RETURNS BIGINT AS $$
DECLARE
v_patient_id        BIGINT;
    v_complexity        complexity_level;
    v_diagnosis_id      BIGINT;
    v_diagnosis_name    VARCHAR(255);
    v_prescription_id   BIGINT;
    v_eligible_diagnoses TEXT[] := ARRAY[
        'Upper Respiratory Tract Infection (Cold)',
        'Influenza (Flu)',
        'Food Poisoning',
        'Acute Gastroenteritis'
    ];
BEGIN
SELECT c.patient_id, c.complexity_level INTO v_patient_id, v_complexity FROM consultations c WHERE c.id = p_consultation_id;

IF v_complexity != 'SIMPLE' THEN
        RAISE EXCEPTION 'AUTO_PRESCRIPTION_NOT_ALLOWED: Automatic prescriptions are only available for simple cases. Current complexity: %', v_complexity;
END IF;

SELECT id, diagnosis_name INTO v_diagnosis_id, v_diagnosis_name
FROM diagnoses WHERE consultation_id = p_consultation_id AND diagnosis_type = 'AUTO_GENERATED'
ORDER BY confidence_score DESC LIMIT 1;

IF NOT (v_diagnosis_name = ANY(v_eligible_diagnoses)) THEN
        RAISE EXCEPTION 'DIAGNOSIS_NOT_ELIGIBLE: Diagnosis "%" does not qualify for an automatic prescription. The patient will be scheduled with a doctor.', v_diagnosis_name;
END IF;

INSERT INTO prescriptions (consultation_id, patient_id, diagnosis_id, issued_at, valid_until, is_auto_generated)
VALUES (p_consultation_id, v_patient_id, v_diagnosis_id, NOW(), NOW() + INTERVAL '7 days', TRUE)
    RETURNING id INTO v_prescription_id;

IF v_diagnosis_name IN ('Upper Respiratory Tract Infection (Cold)', 'Influenza (Flu)') THEN
        INSERT INTO prescription_medications (prescription_id, medication_name, dosage, frequency, duration_days, instructions) VALUES
            (v_prescription_id, 'Paracetamol 500 mg', '1 tablet', 'Every 6–8 hours as needed', 5, 'Do not exceed 4 tablets per day. Take with food or water.'),
            (v_prescription_id, 'Vitamin C 1000 mg (effervescent)', '1 tablet', 'Once daily', 7, 'Dissolve in a glass of water. Take after meals.'),
            (v_prescription_id, 'Ibuprofen 400 mg', '1 tablet', 'Every 8 hours as needed', 3, 'Take with food. Contraindicated if you have a peptic ulcer or kidney issues.');
END IF;

    IF v_diagnosis_name IN ('Food Poisoning', 'Acute Gastroenteritis') THEN
        INSERT INTO prescription_medications (prescription_id, medication_name, dosage, frequency, duration_days, instructions) VALUES
            (v_prescription_id, 'Diosmectite (Smecta)', '1 sachet', 'Three times daily', 3, 'Dissolve in 100 ml of water. Take between meals.'),
            (v_prescription_id, 'Oral Rehydration Solution', '200–400 ml', 'After each episode of vomiting or diarrhoea', 3, 'Rehydration is essential. Sip slowly — do not drink all at once.'),
            (v_prescription_id, 'Paracetamol 500 mg', '1 tablet', 'Every 6–8 hours as needed', 3, 'Only if you have a fever. Do not exceed 4 tablets per day.');
END IF;

UPDATE consultations SET status = 'COMPLETED' WHERE id = p_consultation_id;
RETURN v_prescription_id;

EXCEPTION WHEN OTHERS THEN
    RAISE EXCEPTION 'PRESCRIPTION_ERROR: Failed to generate prescription for consultation %: %', p_consultation_id, SQLERRM;
END;
$$ LANGUAGE plpgsql;