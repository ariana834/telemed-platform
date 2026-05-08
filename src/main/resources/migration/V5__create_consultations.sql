-- Fiecare consultatie trece printr-o serie de stari (status),de la introducerea simptomelor pana la diagnostic final.
-- Am modelat asta ca un state machine - fiecare stare permite doar anumite tranzitii, verificate prin trigger.
-- Fluxul normal arata asa:
-- PENDING_FORM → FORM_GENERATED → FORM_COMPLETED → DIAGNOSIS_PENDING → SCHEDULED → IN_PROGRESS → COMPLETED
-- Fluxul de urgenta:
-- PENDING_FORM → EMERGENCY_REDIRECT

CREATE TABLE consultations (
                               id                  BIGSERIAL PRIMARY KEY,
                               patient_id          BIGINT              NOT NULL REFERENCES patients(id) ON DELETE RESTRICT,
                               status              consultation_status NOT NULL DEFAULT 'PENDING_FORM',
                               complexity_level    complexity_level,
                               emergency_redirect  BOOLEAN             NOT NULL DEFAULT FALSE,
                               notes               TEXT,
                               created_at          TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
                               updated_at          TIMESTAMPTZ         NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_consultations_patient_id ON consultations(patient_id);
-- caut des dupa status cand listez consultatiile active
CREATE INDEX idx_consultations_status     ON consultations(status);
CREATE INDEX idx_consultations_created_at ON consultations(created_at DESC);

CREATE TRIGGER trg_consultations_updated_at
    BEFORE UPDATE ON consultations
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- adaug triggerul definit in V3 care verifica abonamentul activ
CREATE TRIGGER trg_check_active_subscription
    BEFORE INSERT ON consultations
    FOR EACH ROW EXECUTE FUNCTION check_active_subscription();

-- verific tranzitiile valide intre statusuri
CREATE OR REPLACE FUNCTION validate_consultation_status_transition()
RETURNS TRIGGER AS $$
BEGIN
    -- daca statusul nu s-a schimbat, nu fac nimic
    IF OLD.status = NEW.status THEN
        RETURN NEW;
END IF;

    -- definesc tranzitiile valide ca un set de perechi (vechi -> nou)
    IF NOT (
        (OLD.status = 'PENDING_FORM'      AND NEW.status = 'FORM_GENERATED')      OR
        (OLD.status = 'PENDING_FORM'      AND NEW.status = 'EMERGENCY_REDIRECT')  OR
        (OLD.status = 'FORM_GENERATED'    AND NEW.status = 'FORM_COMPLETED')      OR
        (OLD.status = 'FORM_COMPLETED'    AND NEW.status = 'DIAGNOSIS_PENDING')   OR
        (OLD.status = 'DIAGNOSIS_PENDING' AND NEW.status = 'SCHEDULED')           OR
        (OLD.status = 'DIAGNOSIS_PENDING' AND NEW.status = 'COMPLETED')           OR
        (OLD.status = 'SCHEDULED'         AND NEW.status = 'IN_PROGRESS')         OR
        (OLD.status = 'IN_PROGRESS'       AND NEW.status = 'COMPLETED')           OR
        (NEW.status = 'CANCELLED')
    ) THEN
        RAISE EXCEPTION 'INVALID_STATUS_TRANSITION: Tranzitia de la % la % nu este permisa',
            OLD.status, NEW.status;
END IF;
    IF NEW.status = 'EMERGENCY_REDIRECT' THEN
        NEW.emergency_redirect := TRUE;
END IF;

RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validate_status_transition
    BEFORE UPDATE OF status ON consultations
    FOR EACH ROW EXECUTE FUNCTION validate_consultation_status_transition();


CREATE TABLE consultation_symptoms (
                                       id               BIGSERIAL PRIMARY KEY,
                                       consultation_id  BIGINT       NOT NULL REFERENCES consultations(id) ON DELETE CASCADE,
                                       symptom_name     VARCHAR(255) NOT NULL,
                                       severity         VARCHAR(20)  NOT NULL DEFAULT 'MODERATE',
                                       order_index      SMALLINT     NOT NULL,
                                       created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

                                       CONSTRAINT uq_symptom_order   UNIQUE (consultation_id, order_index),
                                       CONSTRAINT chk_order_index    CHECK  (order_index BETWEEN 1 AND 3),
                                       CONSTRAINT chk_severity       CHECK  (severity IN ('MILD', 'MODERATE', 'SEVERE')),
                                       CONSTRAINT uq_symptom_name    UNIQUE (consultation_id, symptom_name)
);

CREATE INDEX idx_symptoms_consultation_id ON consultation_symptoms(consultation_id);
-- indexez pe symptom_name pentru rapoarte si statistici
CREATE INDEX idx_symptoms_name            ON consultation_symptoms(symptom_name);

-- verific ca o consultatie nu poate avea mai mult de 3 simptome
CREATE OR REPLACE FUNCTION check_max_symptoms()
RETURNS TRIGGER AS $$
DECLARE
v_count        INTEGER;
    v_status       consultation_status;
BEGIN
SELECT status INTO v_status
FROM consultations
WHERE id = NEW.consultation_id;

IF v_status != 'PENDING_FORM' THEN
        RAISE EXCEPTION 'SYMPTOMS_LOCKED: Nu se mai pot modifica simptomele pentru consultatia %',
            NEW.consultation_id;
END IF;

SELECT COUNT(*) INTO v_count
FROM consultation_symptoms
WHERE consultation_id = NEW.consultation_id;

IF v_count >= 3 THEN
        RAISE EXCEPTION 'MAX_SYMPTOMS_REACHED: O consultatie poate avea maxim 3 simptome (consultatie: %)',
            NEW.consultation_id;
END IF;

RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_check_max_symptoms
    BEFORE INSERT ON consultation_symptoms
    FOR EACH ROW EXECUTE FUNCTION check_max_symptoms();

-- dupa ce se adauga al 3-lea simptom, trec automat consultatie, in starea FORM_GENERATED si generez fisa medicala.
CREATE OR REPLACE FUNCTION auto_generate_form_on_third_symptom()
RETURNS TRIGGER AS $$
DECLARE
v_count INTEGER;
BEGIN
SELECT COUNT(*) INTO v_count
FROM consultation_symptoms
WHERE consultation_id = NEW.consultation_id;


IF v_count = 2 THEN
UPDATE consultations
SET status = 'FORM_GENERATED'
WHERE id = NEW.consultation_id;

-- generarea efectiva a fisei se face din V8
PERFORM generate_medical_form(NEW.consultation_id);
END IF;

RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_auto_generate_form
    AFTER INSERT ON consultation_symptoms
    FOR EACH ROW EXECUTE FUNCTION auto_generate_form_on_third_symptom();