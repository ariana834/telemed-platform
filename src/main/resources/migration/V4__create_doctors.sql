-- Doctorii sunt tot useri in sistem, dar cu rolul DOCTOR

CREATE TABLE doctors (
                         id               BIGSERIAL PRIMARY KEY,
                         user_id          BIGINT       NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
                         first_name       VARCHAR(100) NOT NULL,
                         last_name        VARCHAR(100) NOT NULL,
                         specialization   VARCHAR(100) NOT NULL,
                         license_number   VARCHAR(50)  NOT NULL UNIQUE,
                         phone            VARCHAR(20),
                         bio              TEXT,
                         is_available     BOOLEAN      NOT NULL DEFAULT TRUE,
                         created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                         updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

                         CONSTRAINT chk_doctor_phone CHECK (phone IS NULL OR phone ~ '^\+?[0-9\s\-]{7,20}$')
    );

CREATE INDEX idx_doctors_user_id        ON doctors(user_id);
CREATE INDEX idx_doctors_specialization ON doctors(specialization);
CREATE INDEX idx_doctors_is_available   ON doctors(is_available);

CREATE TRIGGER trg_doctors_updated_at
    BEFORE UPDATE ON doctors
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

--verific daca chiar are rolul doctor
CREATE OR REPLACE FUNCTION validate_doctor_user_role()
RETURNS TRIGGER AS $$
DECLARE
v_role user_role;
BEGIN
SELECT role INTO v_role FROM users WHERE id = NEW.user_id;

IF v_role != 'DOCTOR' THEN
        RAISE EXCEPTION 'INVALID_USER_ROLE: Userul % nu are rolul DOCTOR', NEW.user_id;
END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validate_doctor_role
    BEFORE INSERT ON doctors
    FOR EACH ROW EXECUTE FUNCTION validate_doctor_user_role();

-- Fiecare doctor are un program saptamanal predefinit, am ales sa stochez ziua ca INTEGER (0=Luni, 6=Duminica)
CREATE TABLE doctor_schedules (
                                  id           BIGSERIAL PRIMARY KEY,
                                  doctor_id    BIGINT    NOT NULL REFERENCES doctors(id) ON DELETE CASCADE,
                                  day_of_week  SMALLINT  NOT NULL,
                                  start_time   TIME      NOT NULL,
                                  end_time     TIME      NOT NULL,
                                  is_active    BOOLEAN   NOT NULL DEFAULT TRUE,
                                  created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                                  CONSTRAINT chk_day_of_week  CHECK (day_of_week BETWEEN 0 AND 6),
                                  CONSTRAINT chk_time_range   CHECK (end_time > start_time),
                                  CONSTRAINT chk_min_duration CHECK (
                                      EXTRACT(EPOCH FROM (end_time - start_time)) / 60 >= 30
                                      )
);

CREATE INDEX idx_doctor_schedules_doctor_id    ON doctor_schedules(doctor_id);
-- indexez pe (doctor_id, day_of_week) pentru ca algoritmul de programare
-- va cauta mereu dupa aceasta combinatie
CREATE INDEX idx_doctor_schedules_day          ON doctor_schedules(doctor_id, day_of_week);

-- nu permit doua intervale care se suprapun pentru acelasi doctor
-- in aceeasi zi - ar crea confuzie la programare
CREATE OR REPLACE FUNCTION check_schedule_overlap()
RETURNS TRIGGER AS $$
DECLARE
v_overlap BOOLEAN;
BEGIN
SELECT EXISTS (
    SELECT 1
    FROM doctor_schedules
    WHERE doctor_id    = NEW.doctor_id
      AND day_of_week  = NEW.day_of_week
      AND is_active     = TRUE
      AND id            != COALESCE(NEW.id, -1)
          AND (
              -- noul interval incepe in mijlocul unui interval existent
              (NEW.start_time >= start_time AND NEW.start_time < end_time)
              OR
              -- noul interval se termina in mijlocul unui interval existent
              (NEW.end_time > start_time AND NEW.end_time <= end_time)
              OR
              -- noul interval acopera complet un interval existent
              (NEW.start_time <= start_time AND NEW.end_time >= end_time)
          )
) INTO v_overlap;

IF v_overlap THEN
        RAISE EXCEPTION 'SCHEDULE_OVERLAP: Doctorul % are deja un interval care se suprapune in ziua %',
            NEW.doctor_id, NEW.day_of_week;
END IF;

RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_check_schedule_overlap
    BEFORE INSERT OR UPDATE ON doctor_schedules
                         FOR EACH ROW EXECUTE FUNCTION check_schedule_overlap();


-- returneaza toti doctorii disponibili, intr-un anumit interval de timp, tinand cont de program.
CREATE OR REPLACE FUNCTION get_available_doctors(
    p_day_of_week  SMALLINT,
    p_start_time   TIME,
    p_end_time     TIME
)
RETURNS TABLE (
    doctor_id      BIGINT,
    first_name     VARCHAR,
    last_name      VARCHAR,
    specialization VARCHAR
) AS $$
BEGIN
RETURN QUERY
SELECT
    d.id,
    d.first_name,
    d.last_name,
    d.specialization
FROM doctors d
         JOIN doctor_schedules ds ON ds.doctor_id = d.id
WHERE d.is_available    = TRUE
  AND ds.day_of_week    = p_day_of_week
  AND ds.is_active      = TRUE
  AND ds.start_time    <= p_start_time
  AND ds.end_time      >= p_end_time
ORDER BY d.last_name;
END;
$$ LANGUAGE plpgsql;