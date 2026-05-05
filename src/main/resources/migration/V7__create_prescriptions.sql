-- Programarile leaga o consultatie de un doctor si un interval orar.
-- Algoritmul de programare automata (din V8) gaseste primul slot liber
-- tinand cont de complexitatea cazului si programul doctorilor.
-- Durata consultatiei este determinata de complexitate:
-- SIMPLE    = 10 minute
-- MEDIUM    = 20 minute
-- COMPLEX   = 30 minute
-- EMERGENCY = redirectionat, nu se programeaza

CREATE TABLE appointments (
                              id                BIGSERIAL          PRIMARY KEY,
                              consultation_id   BIGINT             NOT NULL UNIQUE REFERENCES consultations(id) ON DELETE RESTRICT,
                              doctor_id         BIGINT             NOT NULL REFERENCES doctors(id) ON DELETE RESTRICT,
                              patient_id        BIGINT             NOT NULL REFERENCES patients(id) ON DELETE RESTRICT,
                              start_time        TIMESTAMPTZ        NOT NULL,
                              end_time          TIMESTAMPTZ        NOT NULL,
                              duration_minutes  SMALLINT           NOT NULL,
                              status            appointment_status NOT NULL DEFAULT 'SCHEDULED',
                              notes             TEXT,
                              created_at        TIMESTAMPTZ        NOT NULL DEFAULT NOW(),
                              updated_at        TIMESTAMPTZ        NOT NULL DEFAULT NOW(),

                              CONSTRAINT chk_appointment_times    CHECK (end_time > start_time),
                              CONSTRAINT chk_duration             CHECK (duration_minutes IN (10, 20, 30)),
                              CONSTRAINT chk_not_in_past          CHECK (start_time >= NOW() - INTERVAL '1 minute'),
    -- verific ca durata corespunde cu intervalul
    CONSTRAINT chk_duration_matches     CHECK (
        EXTRACT(EPOCH FROM (end_time - start_time)) / 60 = duration_minutes
    )
);

CREATE INDEX idx_appointments_consultation_id ON appointments(consultation_id);
CREATE INDEX idx_appointments_doctor_id       ON appointments(doctor_id);
CREATE INDEX idx_appointments_patient_id      ON appointments(patient_id);
-- indexez pe start_time pentru ca algoritmul de programare
-- cauta des dupa intervale de timp
CREATE INDEX idx_appointments_start_time      ON appointments(start_time);
CREATE INDEX idx_appointments_status          ON appointments(status);

CREATE TRIGGER trg_appointments_updated_at
    BEFORE UPDATE ON appointments
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- verific ca doctorul este disponibil in intervalul programat.
-- tin cont atat de programul sau saptamanal cat si de
-- programarile deja existente in acel interval.
CREATE OR REPLACE FUNCTION check_doctor_availability()
RETURNS TRIGGER AS $$
DECLARE
v_day_of_week     SMALLINT;
    v_start_time      TIME;
    v_end_time        TIME;
    v_in_schedule     BOOLEAN;
    v_has_overlap     BOOLEAN;
BEGIN
    v_day_of_week := EXTRACT(ISODOW FROM NEW.start_time)::SMALLINT - 1;
    v_start_time  := NEW.start_time::TIME;
    v_end_time    := NEW.end_time::TIME;

    -- verific daca intervalul este in programul doctorului
SELECT EXISTS (
    SELECT 1 FROM doctor_schedules
    WHERE doctor_id   = NEW.doctor_id
      AND day_of_week = v_day_of_week
      AND is_active   = TRUE
      AND start_time  <= v_start_time
      AND end_time    >= v_end_time
) INTO v_in_schedule;

IF NOT v_in_schedule THEN
        RAISE EXCEPTION 'DOCTOR_NOT_SCHEDULED: Doctorul % nu este disponibil in intervalul ales',
            NEW.doctor_id;
END IF;

    -- verific daca doctorul are deja o programare in acel interval
SELECT EXISTS (
    SELECT 1 FROM appointments
    WHERE doctor_id = NEW.doctor_id
      AND id        != COALESCE(NEW.id, -1)
          AND status    NOT IN ('CANCELLED', 'NO_SHOW')
          AND (
              (NEW.start_time >= start_time AND NEW.start_time < end_time)
              OR
              (NEW.end_time > start_time AND NEW.end_time <= end_time)
              OR
              (NEW.start_time <= start_time AND NEW.end_time >= end_time)
          )
) INTO v_has_overlap;

IF v_has_overlap THEN
        RAISE EXCEPTION 'APPOINTMENT_UNAVAILABLE: Doctorul % are deja o programare in acest interval',
            NEW.doctor_id;
END IF;

RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_check_doctor_availability
    BEFORE INSERT OR UPDATE ON appointments
                         FOR EACH ROW EXECUTE FUNCTION check_doctor_availability();

-- cand o programare devine IN_PROGRESS, actualizez statusul consultatiei
CREATE OR REPLACE FUNCTION sync_consultation_status_with_appointment()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status = 'IN_PROGRESS' AND OLD.status = 'SCHEDULED' THEN
UPDATE consultations
SET status = 'IN_PROGRESS'
WHERE id = NEW.consultation_id;
END IF;

    IF NEW.status = 'CANCELLED' THEN
UPDATE consultations
SET status = 'CANCELLED'
WHERE id = NEW.consultation_id
  AND status NOT IN ('COMPLETED', 'EMERGENCY_REDIRECT');
END IF;

RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_sync_consultation_status
    AFTER UPDATE OF status ON appointments
    FOR EACH ROW EXECUTE FUNCTION sync_consultation_status_with_appointment();