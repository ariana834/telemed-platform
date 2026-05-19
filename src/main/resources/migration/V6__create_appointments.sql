-- Fisa medicala este generata automat de sistem dupa ce pacientul, declara cele 3 simptome. Fiecare consultatie are propria fisa,
-- formata din intrebari generate dinamic de functia din V8.
-- Am ales sa stochez intrebarile si raspunsurile in tabele separate, pentru a putea analiza raspunsurile individual si a le reutilizain consultatii viitoare ale aceluiasi pacient.

CREATE TABLE medical_form_questions (
                                        id               BIGSERIAL     PRIMARY KEY,
                                        consultation_id  BIGINT        NOT NULL REFERENCES consultations(id) ON DELETE CASCADE,
                                        question_text    TEXT          NOT NULL,
                                        question_type    question_type NOT NULL,
                                        options          JSONB,
                                        order_index      SMALLINT      NOT NULL,
                                        is_required      BOOLEAN       NOT NULL DEFAULT TRUE,
                                        created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

                                        CONSTRAINT uq_question_order UNIQUE (consultation_id, order_index),
                                        CONSTRAINT chk_options_for_choice CHECK (
                                            -- daca tipul e MULTIPLE_CHOICE sau CHECKBOX, options nu poate fi null
                                            (question_type IN ('MULTIPLE_CHOICE', 'CHECKBOX') AND options IS NOT NULL)
                                                OR
                                            (question_type IN ('OPEN_TEXT', 'YES_NO'))
                                            )
);

CREATE INDEX idx_questions_consultation_id ON medical_form_questions(consultation_id);


-- Raspunsurile pacientului la fisa medicala.
-- Un pacient poate raspunde o singura data la fiecare intrebare.
-- Stochez raspunsul ca TEXT indiferent de tipul intrebarii:
-- pentru YES_NO: "YES" sau "NO"
-- pentru MULTIPLE_CHOICE: valoarea aleasa
-- pentru CHECKBOX: valori separate prin virgula
-- pentru OPEN_TEXT: raspunsul liber

CREATE TABLE medical_form_answers (
                                      id               BIGSERIAL   PRIMARY KEY,
                                      question_id      BIGINT      NOT NULL REFERENCES medical_form_questions(id) ON DELETE CASCADE,
                                      consultation_id  BIGINT      NOT NULL REFERENCES consultations(id) ON DELETE CASCADE,
                                      answer_text      TEXT        NOT NULL,
                                      created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                                      -- un pacient raspunde o singura data la fiecare intrebare
                                      CONSTRAINT uq_one_answer_per_question UNIQUE (question_id, consultation_id)
);

CREATE INDEX idx_answers_consultation_id ON medical_form_answers(consultation_id);
CREATE INDEX idx_answers_question_id     ON medical_form_answers(question_id);

-- dupa ce pacientul raspunde la toate intrebarile obligatorii,
-- trec automat consultatie in FORM_COMPLETED
CREATE OR REPLACE FUNCTION check_form_completion()
RETURNS TRIGGER AS $$
DECLARE
v_total_required   INTEGER;
    v_total_answered   INTEGER;
BEGIN
    -- numar intrebarile obligatorii
SELECT COUNT(*) INTO v_total_required
FROM medical_form_questions
WHERE consultation_id = NEW.consultation_id
  AND is_required = TRUE;

-- numar raspunsurile date la intrebarile obligatorii
SELECT COUNT(*) INTO v_total_answered
FROM medical_form_answers a
         JOIN medical_form_questions q ON q.id = a.question_id
WHERE a.consultation_id = NEW.consultation_id
  AND q.is_required = TRUE;

-- +1 pentru ca NEW inca nu e in tabel
IF v_total_answered + 1 >= v_total_required THEN
UPDATE consultations
SET status = 'FORM_COMPLETED'
WHERE id = NEW.consultation_id
  AND status = 'FORM_GENERATED';
END IF;

RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_check_form_completion
    AFTER INSERT ON medical_form_answers
    FOR EACH ROW EXECUTE FUNCTION check_form_completion();


-- Diagnosticele pot fi de 3 tipuri:
-- AUTO_GENERATED - generate de sistem dupa completarea fisei
-- PRELIMINARY    - confirmate provizoriu de sistem inainte de consultatie
-- CONFIRMED      - confirmate sau modificate de doctor dupa consultatie

CREATE TABLE diagnoses (
                           id                BIGSERIAL        PRIMARY KEY,
                           consultation_id   BIGINT           NOT NULL REFERENCES consultations(id) ON DELETE RESTRICT,
                           diagnosis_name    VARCHAR(255)     NOT NULL,
                           diagnosis_type    diagnosis_type   NOT NULL,
                           complexity_level  complexity_level NOT NULL,
    -- codul ICD-10 este optional, dar util pentru rapoarte medicale
                           icd_code          VARCHAR(10),
                           confidence_score  SMALLINT,
                           notes             TEXT,
    -- NULL daca e generat automat, altfel id-ul doctorului
                           created_by        BIGINT           REFERENCES users(id) ON DELETE SET NULL,
                           created_at        TIMESTAMPTZ      NOT NULL DEFAULT NOW(),

                           CONSTRAINT chk_confidence_score CHECK (
                               confidence_score IS NULL OR confidence_score BETWEEN 0 AND 100
                               ),
    -- maxim 2 diagnostice auto/preliminary per consultatie
                           CONSTRAINT chk_auto_diagnosis_limit CHECK (
                               diagnosis_type = 'CONFIRMED' OR id IS NOT NULL
                               )
);

CREATE INDEX idx_diagnoses_consultation_id ON diagnoses(consultation_id);
CREATE INDEX idx_diagnoses_type            ON diagnoses(diagnosis_type);


CREATE OR REPLACE FUNCTION check_diagnosis_limit()
RETURNS TRIGGER AS $$
DECLARE
v_count INTEGER;
BEGIN
    IF NEW.diagnosis_type IN ('AUTO_GENERATED', 'PRELIMINARY') THEN
SELECT COUNT(*) INTO v_count
FROM diagnoses
WHERE consultation_id = NEW.consultation_id
  AND diagnosis_type IN ('AUTO_GENERATED', 'PRELIMINARY');

IF v_count >= 2 THEN
            RAISE EXCEPTION 'DIAGNOSIS_LIMIT_REACHED: O consultatie poate avea maxim 2 diagnostice automate (consultatie: %)',
                NEW.consultation_id;
END IF;
END IF;

    IF NEW.diagnosis_type = 'CONFIRMED' THEN
SELECT COUNT(*) INTO v_count
FROM diagnoses
WHERE consultation_id = NEW.consultation_id
  AND diagnosis_type = 'CONFIRMED';

IF v_count >= 1 THEN
            RAISE EXCEPTION 'CONFIRMED_DIAGNOSIS_EXISTS: Consultatia % are deja un diagnostic confirmat',
                NEW.consultation_id;
END IF;
END IF;

RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_check_diagnosis_limit
    BEFORE INSERT ON diagnoses
    FOR EACH ROW EXECUTE FUNCTION check_diagnosis_limit();

-- cand se adauga un diagnostic confirmat de doctor, trec consultatie in COMPLETED automat
CREATE OR REPLACE FUNCTION complete_consultation_on_diagnosis()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.diagnosis_type = 'CONFIRMED' THEN
UPDATE consultations
SET status = 'COMPLETED'
WHERE id = NEW.consultation_id
  AND status = 'IN_PROGRESS';
END IF;

RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_complete_consultation
    AFTER INSERT ON diagnoses
    FOR EACH ROW EXECUTE FUNCTION complete_consultation_on_diagnosis();