
-- Contine algoritmii principali implementati:
-- 1. generate_medical_form     - genereaza fisa medicala bazat pe simptome + istoric
-- 2. compute_preliminary_diagnosis - calculeaza diagnosticul prin sistem de scoruri
-- 3. schedule_next_appointment - gaseste primul slot liber tinand cont de toti doctorii
-- 4. auto_generate_prescription - genereaza reteta automata pentru cazuri simple (fara antibiotice)


--o apelam cu triggerul de la v5
CREATE OR REPLACE FUNCTION generate_medical_form(p_consultation_id BIGINT)
RETURNS VOID AS $$
DECLARE
v_patient_id          BIGINT;
    v_age_category        age_category;
    v_is_child            BOOLEAN;
    v_symptoms_text       TEXT;
    v_abdominal_severity  VARCHAR(20);
    v_order_idx           SMALLINT := 1;

    -- flags simptome detectate prin pattern matching
    v_has_fever           BOOLEAN := FALSE;
    v_has_abdominal       BOOLEAN := FALSE;
    v_has_headache        BOOLEAN := FALSE;
    v_has_vomiting        BOOLEAN := FALSE;
    v_has_no_appetite     BOOLEAN := FALSE;
    v_has_cough           BOOLEAN := FALSE;
    v_has_chest_pain      BOOLEAN := FALSE;
    v_has_fatigue         BOOLEAN := FALSE;
    v_has_rash            BOOLEAN := FALSE;

    -- flags afectiuni cronice cunoscute
    v_has_diabetes        BOOLEAN := FALSE;
    v_has_hypertension    BOOLEAN := FALSE;
    v_has_digestive       BOOLEAN := FALSE;
    v_has_allergies       BOOLEAN := FALSE;

BEGIN
    -- preiau datele pacientului
SELECT c.patient_id, p.age_category
INTO v_patient_id, v_age_category
FROM consultations c
         JOIN patients p ON p.id = c.patient_id
WHERE c.id = p_consultation_id;

v_is_child := (v_age_category = 'CHILD');

    -- concatenez toate simptomele intr-un singur text pentru pattern matching
SELECT string_agg(LOWER(symptom_name), ' ')
INTO v_symptoms_text
FROM consultation_symptoms
WHERE consultation_id = p_consultation_id;

-- detectez simptomele prin ILIKE - suficient de flexibil pentru input liber
v_has_fever      := v_symptoms_text ILIKE '%febr%'     OR v_symptoms_text ILIKE '%temperatur%';
    v_has_abdominal  := v_symptoms_text ILIKE '%abdomin%'  OR v_symptoms_text ILIKE '%burt%'  OR v_symptoms_text ILIKE '%stomac%';
    v_has_headache   := v_symptoms_text ILIKE '%cap%'      OR v_symptoms_text ILIKE '%cefalee%';
    v_has_vomiting   := v_symptoms_text ILIKE '%varsatur%' OR v_symptoms_text ILIKE '%greata%' OR v_symptoms_text ILIKE '%voma%';
    v_has_no_appetite:= v_symptoms_text ILIKE '%pofta%'    OR v_symptoms_text ILIKE '%apetit%';
    v_has_cough      := v_symptoms_text ILIKE '%tuse%';
    v_has_chest_pain := v_symptoms_text ILIKE '%piept%'    OR v_symptoms_text ILIKE '%torac%';
    v_has_fatigue    := v_symptoms_text ILIKE '%oboseala%' OR v_symptoms_text ILIKE '%slabiciune%';
    v_has_rash       := v_symptoms_text ILIKE '%eruptie%'  OR v_symptoms_text ILIKE '%pete%'   OR v_symptoms_text ILIKE '%rash%';

    -- preiau severitatea durerii abdominale daca exista
SELECT severity INTO v_abdominal_severity
FROM consultation_symptoms
WHERE consultation_id = p_consultation_id
  AND (LOWER(symptom_name) ILIKE '%abdomin%' OR LOWER(symptom_name) ILIKE '%burt%')
    LIMIT 1;

-- preiau afectiunile cronice active ale pacientului
-- acestea influenteaza intrebarile generate
SELECT
    BOOL_OR(LOWER(condition_name) ILIKE '%diabet%'),
    BOOL_OR(LOWER(condition_name) ILIKE '%hipertensiune%' OR LOWER(condition_name) ILIKE '%tensiune%'),
    BOOL_OR(LOWER(condition_name) ILIKE '%digestiv%' OR LOWER(condition_name) ILIKE '%gastrit%'),
    BOOL_OR(LOWER(condition_name) ILIKE '%alergi%')
INTO v_has_diabetes, v_has_hypertension, v_has_digestive, v_has_allergies
FROM chronic_conditions
WHERE patient_id = v_patient_id AND is_active = TRUE;

-- ================================================================
-- REGULA 1: URGENTA - durere abdominala severa + varsaturi
-- posibila apendicita - pacientul este redirectionat catre urgente
-- ================================================================
IF v_has_abdominal AND v_has_vomiting
       AND (v_abdominal_severity = 'SEVERE' OR NOT v_has_fever) THEN

UPDATE consultations
SET status = 'EMERGENCY_REDIRECT',
    complexity_level = 'EMERGENCY',
    emergency_redirect = TRUE
WHERE id = p_consultation_id;

-- generam doar intrebari minime de triaj
INSERT INTO medical_form_questions
(consultation_id, question_text, question_type, options, order_index, is_required)
VALUES
    (p_consultation_id, 'Durerea este localizata in partea dreapta jos a abdomenului?',
     'YES_NO', NULL, 1, TRUE),
    (p_consultation_id, 'De cat timp au inceput simptomele?',
     'MULTIPLE_CHOICE', '["Sub 1 ora", "1-6 ore", "6-12 ore", "Peste 12 ore"]', 2, TRUE),
    (p_consultation_id, 'Durerea s-a intensificat progresiv de la debut?',
     'YES_NO', NULL, 3, TRUE),
    (p_consultation_id, 'Aveti febra?',
     'YES_NO', NULL, 4, TRUE);

RAISE NOTICE 'EMERGENCY_REDIRECT: Consultatia % redirectionata la urgente (posibila apendicita)',
            p_consultation_id;
        RETURN;
END IF;

    -- ================================================================
    -- INTREBARI GENERALE - prezente in orice fisa non-urgenta
    -- ================================================================
INSERT INTO medical_form_questions
(consultation_id, question_text, question_type, options, order_index, is_required)
VALUES
    (p_consultation_id, 'De cate zile aveti aceste simptome?',
     'MULTIPLE_CHOICE', '["1 zi", "2-3 zile", "4-7 zile", "Peste o saptamana"]',
     v_order_idx, TRUE);
v_order_idx := v_order_idx + 1;

INSERT INTO medical_form_questions
(consultation_id, question_text, question_type, options, order_index, is_required)
VALUES
    (p_consultation_id, 'Cum ati evalua intensitatea simptomelor?',
     'MULTIPLE_CHOICE',
     '["Usoare - pot desfasura activitati normale", "Moderate - activitatile sunt afectate", "Severe - nu pot desfasura activitati"]',
     v_order_idx, TRUE);
v_order_idx := v_order_idx + 1;

    IF v_has_fever AND v_has_abdominal AND v_has_vomiting THEN

        INSERT INTO medical_form_questions
            (consultation_id, question_text, question_type, options, order_index, is_required)
        VALUES
            (p_consultation_id, 'Ce temperatura aveti?',
             'MULTIPLE_CHOICE',
             '["37-37.5°C (subfebrilitate)", "37.5-38.5°C (febra medie)", "38.5-39.5°C (febra mare)", "Peste 39.5°C"]',
             v_order_idx, TRUE),
            (p_consultation_id, 'Ati consumat alimente posibil alterate in ultimele 24 ore?',
             'YES_NO', NULL, v_order_idx + 1, TRUE),
            (p_consultation_id, 'Aveti diaree?',
             'YES_NO', NULL, v_order_idx + 2, TRUE),
            (p_consultation_id, 'Mai sunt si alte persoane din anturaj cu aceleasi simptome?',
             'YES_NO', NULL, v_order_idx + 3, TRUE),
            (p_consultation_id, 'Varsaturile sunt frecvente (mai mult de 3 episoade pe zi)?',
             'YES_NO', NULL, v_order_idx + 4, FALSE);
        v_order_idx := v_order_idx + 5;

        -- daca are afectiuni digestive cronice adaug intrebari suplimentare
        IF v_has_digestive THEN
            INSERT INTO medical_form_questions
                (consultation_id, question_text, question_type, options, order_index, is_required)
            VALUES
                (p_consultation_id, 'Simptomele actuale seamana cu episoadele anterioare ale afectiunii digestive?',
                 'YES_NO', NULL, v_order_idx, FALSE),
                (p_consultation_id, 'Urmati un tratament pentru afectiunea digestiva cronica?',
                 'YES_NO', NULL, v_order_idx + 1, TRUE);
            v_order_idx := v_order_idx + 2;
END IF;

UPDATE consultations SET complexity_level = 'MEDIUM' WHERE id = p_consultation_id;
RETURN;
END IF;

    -- ================================================================
    -- REGULA 3: FEBRA + DURERI DE CAP + TUSE sau OBOSEALA
    -- potential viroza respiratorie sau gripa
    -- ================================================================
    IF v_has_fever AND v_has_headache AND (v_has_cough OR v_has_fatigue) THEN

        INSERT INTO medical_form_questions
            (consultation_id, question_text, question_type, options, order_index, is_required)
        VALUES
            (p_consultation_id, 'Ce temperatura aveti?',
             'MULTIPLE_CHOICE',
             '["37-37.5°C (subfebrilitate)", "37.5-38.5°C (febra medie)", "38.5-39.5°C (febra mare)", "Peste 39.5°C"]',
             v_order_idx, TRUE),
            (p_consultation_id, 'Aveti dureri musculare sau articulare?',
             'YES_NO', NULL, v_order_idx + 1, TRUE),
            (p_consultation_id, 'Aveti dureri in gat sau dificultate la inghitire?',
             'YES_NO', NULL, v_order_idx + 2, TRUE),
            (p_consultation_id, 'Care dintre urmatoarele simptome nazale le aveti?',
             'CHECKBOX',
             '["Nas infundat", "Secretii nazale transparente", "Secretii nazale galbene/verzi", "Niciunul"]',
             v_order_idx + 3, FALSE),
            (p_consultation_id, 'Ati fost vaccinat antigripal in acest sezon?',
             'YES_NO', NULL, v_order_idx + 4, FALSE);
        v_order_idx := v_order_idx + 5;

        -- pacientii hipertensivi au risc mai mare la gripa
        IF v_has_hypertension THEN
            INSERT INTO medical_form_questions
                (consultation_id, question_text, question_type, options, order_index, is_required)
            VALUES
                (p_consultation_id, 'Ati masurat tensiunea arteriala? Ce valori ati obtinut?',
                 'OPEN_TEXT', NULL, v_order_idx, TRUE);
            v_order_idx := v_order_idx + 1;
END IF;

UPDATE consultations SET complexity_level = 'SIMPLE' WHERE id = p_consultation_id;
RETURN;
END IF;

    -- ================================================================
    -- REGULA 4: COPIL + FEBRA + VARSATURI sau LIPSA POFTA
    -- potential boli ale copilariei sau afectiuni digestive pediatrice
    -- ================================================================
    IF v_is_child AND v_has_fever AND (v_has_vomiting OR v_has_no_appetite) THEN

        INSERT INTO medical_form_questions
            (consultation_id, question_text, question_type, options, order_index, is_required)
        VALUES
            (p_consultation_id, 'Copilul are eruptii cutanate sau pete pe piele?',
             'YES_NO', NULL, v_order_idx, TRUE),
            (p_consultation_id, 'Copilul isi trage de urechi sau se plange de dureri in urechi?',
             'YES_NO', NULL, v_order_idx + 1, TRUE),
            (p_consultation_id, 'Ce temperatura are copilul?',
             'MULTIPLE_CHOICE',
             '["37-37.5°C", "37.5-38.5°C", "38.5-39.5°C", "Peste 39.5°C"]',
             v_order_idx + 2, TRUE),
            (p_consultation_id, 'Copilul a fost in contact cu alti copii bolnavi in ultimele 7 zile?',
             'YES_NO', NULL, v_order_idx + 3, TRUE),
            (p_consultation_id, 'Vaccinurile copilului sunt la zi conform schemei nationale?',
             'YES_NO', NULL, v_order_idx + 4, FALSE),
            (p_consultation_id, 'Copilul are dificultati de respiratie sau respira mai rapid decat normal?',
             'YES_NO', NULL, v_order_idx + 5, TRUE);
        v_order_idx := v_order_idx + 6;

UPDATE consultations SET complexity_level = 'MEDIUM' WHERE id = p_consultation_id;
RETURN;
END IF;

    -- ================================================================
    -- REGULA DEFAULT: simptome care nu se incadreaza in tiparele de mai sus
    -- generam intrebari generale + specifice istoricului cronic
    -- ================================================================
INSERT INTO medical_form_questions
(consultation_id, question_text, question_type, options, order_index, is_required)
VALUES
    (p_consultation_id, 'Ati mai avut simptome similare in trecut?',
     'YES_NO', NULL, v_order_idx, FALSE),
    (p_consultation_id, 'Luati in prezent vreun medicament sau supliment?',
     'YES_NO', NULL, v_order_idx + 1, TRUE),
    (p_consultation_id, 'Aveti alergii cunoscute la medicamente?',
     'YES_NO', NULL, v_order_idx + 2, TRUE),
    (p_consultation_id, 'Descrieti pe scurt evolutia simptomelor de la debut pana acum',
     'OPEN_TEXT', NULL, v_order_idx + 3, TRUE);
v_order_idx := v_order_idx + 4;

    IF v_has_diabetes THEN
        INSERT INTO medical_form_questions
            (consultation_id, question_text, question_type, options, order_index, is_required)
        VALUES
            (p_consultation_id, 'Ati monitorizat glicemia in ultimele 24 ore? Ce valori ati obtinut?',
             'OPEN_TEXT', NULL, v_order_idx, TRUE);
        v_order_idx := v_order_idx + 1;
END IF;

    IF v_has_allergies THEN
        INSERT INTO medical_form_questions
            (consultation_id, question_text, question_type, options, order_index, is_required)
        VALUES
            (p_consultation_id, 'Ati fost expus recent la alergeni cunoscuti (praf, polen, alimente, medicamente)?',
             'YES_NO', NULL, v_order_idx, TRUE);
        v_order_idx := v_order_idx + 1;
END IF;

UPDATE consultations SET complexity_level = 'SIMPLE' WHERE id = p_consultation_id;

EXCEPTION
    WHEN OTHERS THEN
        RAISE EXCEPTION 'FORM_GENERATION_ERROR: Eroare la generarea fisei pentru consultatia %: %',
            p_consultation_id, SQLERRM;
END;
$$ LANGUAGE plpgsql;


-- ================================================================
-- 2. CALCUL DIAGNOSTIC PRELIMINAR
-- ================================================================

-- folosesc un sistem de scoruri pentru fiecare diagnostic posibil.
-- fiecare simptom si raspuns la fisa adauga puncte diferitelor diagnostice.
-- diagnosticul cu cel mai mare scor devine diagnosticul principal,
-- al doilea ca scor devine diagnosticul alternativ.
-- abordarea cu scoruri este mai flexibila decat reguli if/else stricte
-- si permite un grad de incertitudine (confidence_score).

CREATE OR REPLACE FUNCTION compute_preliminary_diagnosis(p_consultation_id BIGINT)
RETURNS VOID AS $$
DECLARE
v_patient_id       BIGINT;
    v_age_category     age_category;
    v_complexity       complexity_level;
    v_symptoms_text    TEXT;

    -- scorurile pentru fiecare diagnostic posibil
    v_score_gripa           INTEGER := 0;
    v_score_viroza          INTEGER := 0;
    v_score_toxiinfectie    INTEGER := 0;
    v_score_bronsita        INTEGER := 0;
    v_score_otita           INTEGER := 0;
    v_score_gastroenterita  INTEGER := 0;

    -- raspunsuri cheie extrase din fisa
    v_had_bad_food     BOOLEAN := FALSE;
    v_others_sick      BOOLEAN := FALSE;
    v_high_fever       BOOLEAN := FALSE;
    v_muscle_pain      BOOLEAN := FALSE;
    v_ear_pain         BOOLEAN := FALSE;
    v_has_rash         BOOLEAN := FALSE;
    v_has_diarrhea     BOOLEAN := FALSE;

    v_diagnosis1       VARCHAR(255);
    v_diagnosis2       VARCHAR(255);
    v_icd1             VARCHAR(10);
    v_icd2             VARCHAR(10);
    v_score1           INTEGER;
    v_score2           INTEGER;

BEGIN
SELECT c.patient_id, p.age_category, c.complexity_level
INTO v_patient_id, v_age_category, v_complexity
FROM consultations c
         JOIN patients p ON p.id = c.patient_id
WHERE c.id = p_consultation_id;

-- pentru urgente nu calculam diagnostic complet - doar inseram unul direct
IF v_complexity = 'EMERGENCY' THEN
        INSERT INTO diagnoses
            (consultation_id, diagnosis_name, diagnosis_type, complexity_level, icd_code, confidence_score, notes)
        VALUES
            (p_consultation_id,
             'Posibila apendicita acuta - necesita evaluare chirurgicala de urgenta',
             'AUTO_GENERATED', 'EMERGENCY', 'K37', 75,
             'Simptomele indica o posibila urgenta chirurgicala. Pacientul a fost redirectionat catre UPU.');
        RETURN;
END IF;

SELECT string_agg(LOWER(symptom_name), ' ')
INTO v_symptoms_text
FROM consultation_symptoms
WHERE consultation_id = p_consultation_id;

-- extrag raspunsurile relevante din fisa completata
SELECT
    BOOL_OR(LOWER(a.answer_text) IN ('yes','da') AND q.question_text ILIKE '%alimente%alterate%'),
    BOOL_OR(LOWER(a.answer_text) IN ('yes','da') AND q.question_text ILIKE '%alte persoane%'),
    BOOL_OR(a.answer_text ILIKE '%39%' OR a.answer_text ILIKE '%40%' OR a.answer_text ILIKE '%febra mare%'),
    BOOL_OR(LOWER(a.answer_text) IN ('yes','da') AND q.question_text ILIKE '%musculare%'),
    BOOL_OR(LOWER(a.answer_text) IN ('yes','da') AND q.question_text ILIKE '%urechi%'),
    BOOL_OR(LOWER(a.answer_text) IN ('yes','da') AND q.question_text ILIKE '%eruptii%'),
    BOOL_OR(LOWER(a.answer_text) IN ('yes','da') AND q.question_text ILIKE '%diaree%')
INTO v_had_bad_food, v_others_sick, v_high_fever,
    v_muscle_pain, v_ear_pain, v_has_rash, v_has_diarrhea
FROM medical_form_answers a
         JOIN medical_form_questions q ON q.id = a.question_id
WHERE a.consultation_id = p_consultation_id;

-- ================================================================
-- CALCUL SCORURI
-- fiecare factor adauga o anumita pondere la diagnosticul corespunzator
-- ================================================================

-- GRIPA: febra mare + dureri musculare + contagiozitate
IF v_symptoms_text ILIKE '%febr%'    THEN v_score_gripa := v_score_gripa + 15; END IF;
    IF v_high_fever                       THEN v_score_gripa := v_score_gripa + 20; END IF;
    IF v_muscle_pain                      THEN v_score_gripa := v_score_gripa + 25; END IF;
    IF v_symptoms_text ILIKE '%cap%'     THEN v_score_gripa := v_score_gripa + 10; END IF;
    IF v_others_sick                      THEN v_score_gripa := v_score_gripa + 15; END IF;
    IF v_symptoms_text ILIKE '%oboseala%' THEN v_score_gripa := v_score_gripa + 10; END IF;

    -- VIROZA: febra mica + simptome respiratorii usoare
    IF v_symptoms_text ILIKE '%febr%' AND NOT v_high_fever THEN v_score_viroza := v_score_viroza + 25; END IF;
    IF v_symptoms_text ILIKE '%tuse%'  THEN v_score_viroza := v_score_viroza + 20; END IF;
    IF v_symptoms_text ILIKE '%nas%'   THEN v_score_viroza := v_score_viroza + 15; END IF;
    IF v_symptoms_text ILIKE '%gat%'   THEN v_score_viroza := v_score_viroza + 10; END IF;

    -- TOXIINFECTIE: alimente + varsaturi + diaree
    IF v_had_bad_food                       THEN v_score_toxiinfectie := v_score_toxiinfectie + 35; END IF;
    IF v_symptoms_text ILIKE '%varsatur%'   THEN v_score_toxiinfectie := v_score_toxiinfectie + 20; END IF;
    IF v_has_diarrhea                       THEN v_score_toxiinfectie := v_score_toxiinfectie + 20; END IF;
    IF v_others_sick                        THEN v_score_toxiinfectie := v_score_toxiinfectie + 15; END IF;

    -- GASTROENTERITA: varsaturi + dureri abdominale + febra medie
    IF v_symptoms_text ILIKE '%abdomin%'    THEN v_score_gastroenterita := v_score_gastroenterita + 20; END IF;
    IF v_symptoms_text ILIKE '%varsatur%'   THEN v_score_gastroenterita := v_score_gastroenterita + 20; END IF;
    IF v_symptoms_text ILIKE '%febr%' AND NOT v_high_fever THEN v_score_gastroenterita := v_score_gastroenterita + 15; END IF;
    IF v_has_diarrhea                       THEN v_score_gastroenterita := v_score_gastroenterita + 10; END IF;

    -- BRONSITA: tuse + durere in piept + posibil febra
    IF v_symptoms_text ILIKE '%tuse%'       THEN v_score_bronsita := v_score_bronsita + 30; END IF;
    IF v_symptoms_text ILIKE '%piept%'      THEN v_score_bronsita := v_score_bronsita + 25; END IF;
    IF v_high_fever                          THEN v_score_bronsita := v_score_bronsita + 10; END IF;

    -- OTITA: doar pentru copii cu durere de ureche si febra
    IF v_age_category = 'CHILD' THEN
        IF v_ear_pain                        THEN v_score_otita := v_score_otita + 45; END IF;
        IF v_symptoms_text ILIKE '%febr%'    THEN v_score_otita := v_score_otita + 20; END IF;
        IF v_has_rash                        THEN v_score_otita := v_score_otita - 15; END IF;
END IF;

    -- ================================================================
    -- SELECTEZ TOP 2 DIAGNOSTICE CU SCORURI MAXIME
    -- ================================================================
WITH all_diagnoses AS (
    SELECT * FROM (VALUES
                       ('Gripa',                   'J10', v_score_gripa),
                       ('Viroza respiratorie',     'J06', v_score_viroza),
                       ('Toxiinfectie alimentara', 'A05', v_score_toxiinfectie),
                       ('Gastroenterita acuta',    'A09', v_score_gastroenterita),
                       ('Bronsita acuta',          'J20', v_score_bronsita),
                       ('Otita medie acuta',       'H66', v_score_otita)
                  ) AS t(diag_name, icd, score)
    WHERE score > 20
    ORDER BY score DESC
    LIMIT 2
    ),
    ranked AS (
SELECT *, ROW_NUMBER() OVER (ORDER BY score DESC) AS rn FROM all_diagnoses
    )
SELECT
    MAX(CASE WHEN rn = 1 THEN diag_name END),
    MAX(CASE WHEN rn = 1 THEN icd END),
    MAX(CASE WHEN rn = 1 THEN score END),
    MAX(CASE WHEN rn = 2 THEN diag_name END),
    MAX(CASE WHEN rn = 2 THEN icd END),
    MAX(CASE WHEN rn = 2 THEN score END)
INTO v_diagnosis1, v_icd1, v_score1, v_diagnosis2, v_icd2, v_score2
FROM ranked;

IF v_diagnosis1 IS NOT NULL THEN
        INSERT INTO diagnoses
            (consultation_id, diagnosis_name, diagnosis_type, complexity_level, icd_code, confidence_score)
        VALUES
            (p_consultation_id, v_diagnosis1, 'AUTO_GENERATED', v_complexity, v_icd1,
             LEAST(90, 40 + v_score1 / 2));
END IF;

    IF v_diagnosis2 IS NOT NULL THEN
        INSERT INTO diagnoses
            (consultation_id, diagnosis_name, diagnosis_type, complexity_level, icd_code, confidence_score)
        VALUES
            (p_consultation_id, v_diagnosis2, 'AUTO_GENERATED', v_complexity, v_icd2,
             LEAST(70, 30 + v_score2 / 3));
END IF;

    -- daca niciun diagnostic nu a acumulat scor suficient
    IF v_diagnosis1 IS NULL THEN
        INSERT INTO diagnoses
            (consultation_id, diagnosis_name, diagnosis_type, complexity_level, confidence_score, notes)
        VALUES
            (p_consultation_id, 'Afectiune nespecificata - necesita evaluare medicala',
             'AUTO_GENERATED', 'MEDIUM', 30,
             'Simptomele nu se incadreaza intr-un tipar clar. Este necesara consultatie cu medicul.');
END IF;

UPDATE consultations SET status = 'DIAGNOSIS_PENDING' WHERE id = p_consultation_id;

EXCEPTION
    WHEN OTHERS THEN
        RAISE EXCEPTION 'DIAGNOSIS_ERROR: Eroare la calculul diagnosticului pentru consultatia %: %',
            p_consultation_id, SQLERRM;
END;
$$ LANGUAGE plpgsql;


-- ================================================================
-- 3. PROGRAMARE AUTOMATA
-- ================================================================

-- acesta este algoritmul cel mai complex din sistem.
-- cauta primul slot liber iterand din 10 in 10 minute,
-- tinand cont de:
--   - programul saptamanal al fiecarui doctor
--   - programarile deja existente (fara overlap)
--   - durata consultatiei bazata pe complexitate
-- cauta in urmatoarele 7 zile, returneaza eroare daca nu gaseste nimic.

CREATE OR REPLACE FUNCTION schedule_next_appointment(p_consultation_id BIGINT)
RETURNS BIGINT AS $$
DECLARE
v_patient_id        BIGINT;
    v_complexity        complexity_level;
    v_duration          SMALLINT;
    v_search_from       TIMESTAMPTZ;
    v_search_limit      TIMESTAMPTZ;
    v_found             BOOLEAN := FALSE;
    v_appointment_id    BIGINT;
    v_slot_start        TIMESTAMPTZ;
    v_slot_end          TIMESTAMPTZ;
    v_chosen_doctor     BIGINT;
    r_doctor            RECORD;
    v_day_of_week       SMALLINT;

BEGIN
SELECT patient_id, complexity_level
INTO v_patient_id, v_complexity
FROM consultations
WHERE id = p_consultation_id;

IF v_complexity = 'EMERGENCY' THEN
        RAISE EXCEPTION 'EMERGENCY_NO_APPOINTMENT: Cazurile de urgenta nu se programeaza online';
END IF;

    -- durata variaza in functie de complexitate
    -- SIMPLE = 10 min, MEDIUM = 20 min, COMPLEX = 30 min
    v_duration := CASE v_complexity
        WHEN 'SIMPLE'  THEN 10
        WHEN 'MEDIUM'  THEN 20
        WHEN 'COMPLEX' THEN 30
        ELSE 20
END;

    -- incep cautarea de la urmatoarea ora intreaga dupa momentul curent
    v_search_from  := DATE_TRUNC('hour', NOW()) + INTERVAL '1 hour';
    v_search_limit := v_search_from + INTERVAL '7 days';

    -- iterez prin timpi posibili din 10 in 10 minute
    WHILE v_search_from < v_search_limit AND NOT v_found LOOP

        v_day_of_week := (EXTRACT(ISODOW FROM v_search_from)::SMALLINT - 1);

        -- pentru fiecare doctor care are program in momentul respectiv
FOR r_doctor IN
SELECT d.id AS doctor_id
FROM doctors d
         JOIN doctor_schedules ds ON ds.doctor_id = d.id
WHERE d.is_available = TRUE
  AND ds.day_of_week  = v_day_of_week
  AND ds.is_active    = TRUE
  AND ds.start_time  <= v_search_from::TIME
              AND ds.end_time    >= (v_search_from + (v_duration || ' minutes')::INTERVAL)::TIME
ORDER BY d.id
    LOOP
    -- verific daca doctorul nu are alta programare in acest interval
    -- folosesc tstzrange pentru overlap detection - mai elegant decat comparatii manuale
    IF NOT EXISTS (
    SELECT 1 FROM appointments
    WHERE doctor_id = r_doctor.doctor_id
    AND status NOT IN ('CANCELLED', 'NO_SHOW')
    AND tstzrange(start_time, end_time) &&
    tstzrange(v_search_from, v_search_from + (v_duration || ' minutes')::INTERVAL)
    ) THEN
    v_slot_start   := v_search_from;
v_slot_end     := v_search_from + (v_duration || ' minutes')::INTERVAL;
                v_chosen_doctor := r_doctor.doctor_id;
                v_found        := TRUE;
                EXIT;
END IF;
END LOOP;

        IF NOT v_found THEN
            v_search_from := v_search_from + INTERVAL '10 minutes';
END IF;
END LOOP;

    IF NOT v_found THEN
        RAISE EXCEPTION 'NO_SLOTS_AVAILABLE: Nu exista slot disponibil in urmatoarele 7 zile pentru complexitatea %',
            v_complexity;
END IF;

INSERT INTO appointments
(consultation_id, doctor_id, patient_id, start_time, end_time, duration_minutes, status)
VALUES
    (p_consultation_id, v_chosen_doctor, v_patient_id,
     v_slot_start, v_slot_end, v_duration, 'SCHEDULED')
    RETURNING id INTO v_appointment_id;

UPDATE consultations SET status = 'SCHEDULED' WHERE id = p_consultation_id;

RETURN v_appointment_id;

EXCEPTION
    WHEN OTHERS THEN
        RAISE EXCEPTION 'SCHEDULING_ERROR: Eroare la programarea consultatiei %: %',
            p_consultation_id, SQLERRM;
END;
$$ LANGUAGE plpgsql;


-- ================================================================
-- 4. GENERARE RETETA AUTOMATA
-- ================================================================

-- pentru cazurile simple sistemul poate genera o reteta fara interventia
-- unui doctor. important: folosim DOAR medicamente OTC (fara reteta),
-- fara antibiotice - conform cerintei de business.
-- daca diagnosticul nu se afla in lista de diagnostice simple
-- aruncam exceptie si pacientul este programat la un doctor real.

CREATE OR REPLACE FUNCTION auto_generate_prescription(p_consultation_id BIGINT)
RETURNS BIGINT AS $$
DECLARE
v_patient_id        BIGINT;
    v_complexity        complexity_level;
    v_diagnosis_id      BIGINT;
    v_diagnosis_name    VARCHAR(255);
    v_prescription_id   BIGINT;

    -- lista diagnosticelor care permit reteta automata
    v_simple_diagnoses  TEXT[] := ARRAY[
        'Viroza respiratorie',
        'Gripa',
        'Toxiinfectie alimentara',
        'Gastroenterita acuta'
    ];

BEGIN
SELECT c.patient_id, c.complexity_level
INTO v_patient_id, v_complexity
FROM consultations c
WHERE c.id = p_consultation_id;

IF v_complexity != 'SIMPLE' THEN
        RAISE EXCEPTION 'AUTO_PRESCRIPTION_NOT_ALLOWED: Reteta automata disponibila doar pentru cazuri simple. Complexitate actuala: %',
            v_complexity;
END IF;

    -- preiau diagnosticul cu cel mai mare confidence score
SELECT id, diagnosis_name
INTO v_diagnosis_id, v_diagnosis_name
FROM diagnoses
WHERE consultation_id = p_consultation_id
  AND diagnosis_type  = 'AUTO_GENERATED'
ORDER BY confidence_score DESC
    LIMIT 1;

IF NOT (v_diagnosis_name = ANY(v_simple_diagnoses)) THEN
        RAISE EXCEPTION 'DIAGNOSIS_NOT_ELIGIBLE: Diagnosticul "%" nu permite reteta automata. Pacientul va fi programat la doctor.',
            v_diagnosis_name;
END IF;

INSERT INTO prescriptions
(consultation_id, patient_id, diagnosis_id, issued_at, valid_until, is_auto_generated)
VALUES
    (p_consultation_id, v_patient_id, v_diagnosis_id,
     NOW(), NOW() + INTERVAL '7 days', TRUE)
    RETURNING id INTO v_prescription_id;

-- adaug medicamentele OTC in functie de diagnostic
IF v_diagnosis_name IN ('Viroza respiratorie', 'Gripa') THEN
        INSERT INTO prescription_medications
            (prescription_id, medication_name, dosage, frequency, duration_days, instructions)
        VALUES
            (v_prescription_id, 'Paracetamol 500mg', '1 comprimat', 'La 6-8 ore, la nevoie', 5,
             'Nu depasiti 4 comprimate pe zi. Nu administrati pe stomacul gol.'),
            (v_prescription_id, 'Vitamina C 1000mg', '1 comprimat efervescent', 'O data pe zi', 7,
             'Dizolvati intr-un pahar cu apa. Administrati dupa masa.'),
            (v_prescription_id, 'Ibuprofen 400mg', '1 comprimat', 'La 8 ore, la nevoie', 3,
             'Administrati dupa masa. Contraindicat in caz de ulcer gastric.');
END IF;

    IF v_diagnosis_name IN ('Toxiinfectie alimentara', 'Gastroenterita acuta') THEN
        INSERT INTO prescription_medications
            (prescription_id, medication_name, dosage, frequency, duration_days, instructions)
        VALUES
            (v_prescription_id, 'Smecta (Diosmectita)', '1 plic', 'De 3 ori pe zi', 3,
             'Dizolvati in 100ml apa. Administrati intre mese.'),
            (v_prescription_id, 'Solutie rehidratanta orala', '200-400ml',
             'Dupa fiecare episod de varsaturi sau diaree', 3,
             'Rehidratare esentiala. Beti in inghitituri mici, nu dintr-o data.'),
            (v_prescription_id, 'Paracetamol 500mg', '1 comprimat', 'La 6-8 ore, la nevoie', 3,
             'Doar daca aveti febra. Nu depasiti 4 comprimate pe zi.');
END IF;

UPDATE consultations SET status = 'COMPLETED' WHERE id = p_consultation_id;

RETURN v_prescription_id;

EXCEPTION
    WHEN OTHERS THEN
        RAISE EXCEPTION 'PRESCRIPTION_ERROR: Eroare la generarea retetei pentru consultatia %: %',
            p_consultation_id, SQLERRM;
END;
$$ LANGUAGE plpgsql;