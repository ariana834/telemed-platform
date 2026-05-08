CREATE TABLE prescriptions (
                               id                  BIGSERIAL PRIMARY KEY,
                               consultation_id     BIGINT      NOT NULL REFERENCES consultations(id) ON DELETE RESTRICT,
                               patient_id          BIGINT      NOT NULL REFERENCES patients(id) ON DELETE RESTRICT,
                               doctor_id           BIGINT      REFERENCES doctors(id),
                               diagnosis_id        BIGINT      REFERENCES diagnoses(id),
                               issued_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                               valid_until         TIMESTAMPTZ,
                               is_auto_generated   BOOLEAN     NOT NULL DEFAULT FALSE,
                               status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                               CONSTRAINT chk_prescription_status CHECK (status IN ('ACTIVE', 'USED', 'EXPIRED', 'CANCELLED'))
);

CREATE INDEX idx_prescriptions_consultation_id ON prescriptions(consultation_id);
CREATE INDEX idx_prescriptions_patient_id      ON prescriptions(patient_id);

CREATE TABLE prescription_medications (
                                          id               BIGSERIAL PRIMARY KEY,
                                          prescription_id  BIGINT       NOT NULL REFERENCES prescriptions(id) ON DELETE CASCADE,
                                          medication_name  VARCHAR(255) NOT NULL,
                                          dosage           VARCHAR(100) NOT NULL,
                                          frequency        VARCHAR(100) NOT NULL,
                                          duration_days    SMALLINT,
                                          instructions     TEXT
);

CREATE INDEX idx_prescription_meds_prescription_id ON prescription_medications(prescription_id);

CREATE TABLE referrals (
                           id               BIGSERIAL PRIMARY KEY,
                           consultation_id  BIGINT          NOT NULL REFERENCES consultations(id) ON DELETE RESTRICT,
                           patient_id       BIGINT          NOT NULL REFERENCES patients(id) ON DELETE RESTRICT,
                           doctor_id        BIGINT          REFERENCES doctors(id),
                           referral_type    referral_type   NOT NULL,
                           priority         referral_priority NOT NULL DEFAULT 'ROUTINE',
                           destination      VARCHAR(255),
                           reason           TEXT,
                           issued_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_referrals_consultation_id ON referrals(consultation_id);
CREATE INDEX idx_referrals_patient_id      ON referrals(patient_id);