CREATE TYPE user_role AS ENUM ('PATIENT', 'DOCTOR', 'ADMIN');
CREATE TYPE gender AS ENUM ('MALE', 'FEMALE', 'OTHER');
CREATE TYPE age_category AS ENUM ('CHILD', 'ADULT', 'SENIOR');
CREATE TYPE subscription_type AS ENUM ('MONTHLY', 'ANNUAL');
CREATE TYPE subscription_status AS ENUM ('ACTIVE', 'EXPIRED', 'CANCELLED', 'PENDING');
CREATE TYPE complexity_level AS ENUM ('SIMPLE', 'MEDIUM', 'COMPLEX', 'EMERGENCY');
CREATE TYPE consultation_status AS ENUM (
    'PENDING_FORM', 'FORM_GENERATED', 'FORM_COMPLETED',
    'DIAGNOSIS_PENDING', 'SCHEDULED', 'IN_PROGRESS',
    'COMPLETED', 'CANCELLED', 'EMERGENCY_REDIRECT'
);
CREATE TYPE question_type AS ENUM ('MULTIPLE_CHOICE', 'CHECKBOX', 'OPEN_TEXT', 'YES_NO');
CREATE TYPE diagnosis_type AS ENUM ('PRELIMINARY', 'CONFIRMED', 'AUTO_GENERATED');
CREATE TYPE appointment_status AS ENUM ('SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'NO_SHOW');
CREATE TYPE referral_type AS ENUM ('HOSPITAL', 'INVESTIGATION');
CREATE TYPE referral_priority AS ENUM ('ROUTINE', 'URGENT', 'EMERGENCY');
CREATE TYPE payment_status AS ENUM ('PENDING', 'COMPLETED', 'FAILED', 'REFUNDED');

CREATE TABLE users (
                       id              BIGSERIAL PRIMARY KEY,
                       email           VARCHAR(255) NOT NULL UNIQUE,
                       password_hash   VARCHAR(255) NOT NULL,
                       role            user_role  NOT NULL DEFAULT 'PATIENT',
                       is_active       BOOLEAN   NOT NULL DEFAULT TRUE,
                       created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                       updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

                       CONSTRAINT chk_email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')
    );

-- Index pentru cautari dupa email (login)
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role  ON users(role);

-- trigger: updated_at se seteaza automat la orice update
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();