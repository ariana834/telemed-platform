CREATE TABLE patients (
                          id BIGSERIAL PRIMARY KEY,
                          user_id  BIGINT  NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
                          first_name      VARCHAR(100) NOT NULL,
                          last_name       VARCHAR(100) NOT NULL,
                          birth_date      DATE         NOT NULL,
                          age             INTEGER,
                          age_category    age_category NOT NULL DEFAULT 'ADULT',
                          gender          gender       NOT NULL,
                          blood_type      VARCHAR(5),
                          phone           VARCHAR(20),
                          cnp             VARCHAR(13)  UNIQUE,
                          address         TEXT,
                          notes           TEXT,
                          is_guardian     BOOLEAN      NOT NULL DEFAULT FALSE,
                          created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                          updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

                          CONSTRAINT chk_birth_date   CHECK (birth_date <= CURRENT_DATE),
                          CONSTRAINT chk_blood_type   CHECK (blood_type IN ('A+','A-','B+','B-','AB+','AB-','O+','O-')),
                          CONSTRAINT chk_cnp_length   CHECK (cnp IS NULL OR LENGTH(cnp) = 13),
                          CONSTRAINT chk_phone_format CHECK (phone IS NULL OR phone ~ '^\+?[0-9\s\-]{7,20}$')
    );

CREATE INDEX idx_patients_user_id   ON patients(user_id);
CREATE INDEX idx_patients_cnp       ON patients(cnp);
CREATE INDEX idx_patients_last_name ON patients(last_name);

CREATE TRIGGER trg_patients_updated_at
    BEFORE UPDATE ON patients
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE OR REPLACE FUNCTION calculate_age_category()
RETURNS TRIGGER AS $$
DECLARE
v_age INTEGER;
BEGIN
    v_age := DATE_PART('year', AGE(CURRENT_DATE, NEW.birth_date))::INTEGER;
    NEW.age := v_age;
    IF v_age < 18 THEN
        NEW.age_category := 'CHILD';
    ELSIF v_age >= 65 THEN
        NEW.age_category := 'SENIOR';
ELSE
        NEW.age_category := 'ADULT';
END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_calculate_age_category
    BEFORE INSERT OR UPDATE OF birth_date ON patients
    FOR EACH ROW EXECUTE FUNCTION calculate_age_category();

CREATE TABLE guardians (
                           id                  BIGSERIAL PRIMARY KEY,
                           patient_id          BIGINT       NOT NULL UNIQUE REFERENCES patients(id) ON DELETE CASCADE,
                           guardian_user_id    BIGINT       NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
                           first_name          VARCHAR(100) NOT NULL,
                           last_name           VARCHAR(100) NOT NULL,
                           phone               VARCHAR(20),
                           email               VARCHAR(255),
                           relationship        VARCHAR(50)  NOT NULL DEFAULT 'PARENT',
                           created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

                           CONSTRAINT chk_relationship CHECK (relationship IN ('PARENT', 'LEGAL_GUARDIAN', 'GRANDPARENT', 'OTHER')),
                           CONSTRAINT chk_guardian_phone CHECK (phone IS NULL OR phone ~ '^\+?[0-9\s\-]{7,20}$')
    );

CREATE INDEX idx_guardians_patient_id       ON guardians(patient_id);
CREATE INDEX idx_guardians_guardian_user_id ON guardians(guardian_user_id);

CREATE OR REPLACE FUNCTION validate_guardian_for_child()
RETURNS TRIGGER AS $$
DECLARE
v_age_category age_category;
BEGIN
SELECT age_category INTO v_age_category
FROM patients
WHERE id = NEW.patient_id;

IF v_age_category != 'CHILD' THEN
        RAISE EXCEPTION 'GUARDIAN_ONLY_FOR_CHILD: Cannot assign guardian to non-child patient (id: %)', NEW.patient_id;
END IF;

UPDATE patients SET is_guardian = TRUE WHERE id = NEW.patient_id;

RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validate_guardian
    BEFORE INSERT ON guardians
    FOR EACH ROW EXECUTE FUNCTION validate_guardian_for_child();

CREATE TABLE chronic_conditions (
                                    id              BIGSERIAL PRIMARY KEY,
                                    patient_id      BIGINT       NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
                                    condition_name  VARCHAR(255) NOT NULL,
                                    diagnosed_date  DATE,
                                    severity        VARCHAR(20)  NOT NULL DEFAULT 'MODERATE',
                                    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
                                    notes           TEXT,
                                    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

                                    CONSTRAINT chk_severity       CHECK (severity IN ('MILD', 'MODERATE', 'SEVERE')),
                                    CONSTRAINT chk_diagnosed_date CHECK (diagnosed_date IS NULL OR diagnosed_date <= CURRENT_DATE),
                                    CONSTRAINT uq_active_condition UNIQUE (patient_id, condition_name, is_active)
);

CREATE INDEX idx_chronic_conditions_patient_id ON chronic_conditions(patient_id);
CREATE INDEX idx_chronic_conditions_active     ON chronic_conditions(patient_id, is_active);