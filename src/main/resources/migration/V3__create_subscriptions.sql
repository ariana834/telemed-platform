-- Aici tin evidenta abonamentelor pacientilor.
-- Un pacient poate avea un singur abonament activ la un moment dat,
-- dar poate avea istoric de abonamente anterioare (lunar sau anual).


CREATE TABLE subscriptions (
                               id              BIGSERIAL PRIMARY KEY,
                               patient_id      BIGINT              NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
                               type            subscription_type   NOT NULL,
                               start_date      DATE                NOT NULL DEFAULT CURRENT_DATE,
                               end_date        DATE                NOT NULL,
                               status          subscription_status NOT NULL DEFAULT 'PENDING',
                               price           NUMERIC(10, 2)      NOT NULL,
                               created_at      TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
                               updated_at      TIMESTAMPTZ         NOT NULL DEFAULT NOW(),

    -- un pacient nu poate avea doua abonamente active in acelasi timp
                               CONSTRAINT uq_one_active_subscription UNIQUE (patient_id, status),
                               CONSTRAINT chk_dates   CHECK (end_date > start_date),
                               CONSTRAINT chk_price   CHECK (price > 0)
);

CREATE INDEX idx_subscriptions_patient_id ON subscriptions(patient_id);
-- indexez si pe status pentru ca voi cauta des dupa status = ACTIVE
CREATE INDEX idx_subscriptions_status    ON subscriptions(status);

CREATE TRIGGER trg_subscriptions_updated_at
    BEFORE UPDATE ON subscriptions
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- end_date se calculeaza automat in functie de tipul abonamentului
-- lunar = +1 luna, anual = +1 an de la data de start
CREATE OR REPLACE FUNCTION set_subscription_end_date()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.type = 'MONTHLY' THEN
        NEW.end_date := NEW.start_date + INTERVAL '1 month';
    ELSIF NEW.type = 'ANNUAL' THEN
        NEW.end_date := NEW.start_date + INTERVAL '1 year';
END IF;

    -- setez pretul automat in functie de tip
    -- lunar = 50 RON, anual = 500 RON
    IF NEW.price IS NULL OR NEW.price = 0 THEN
        IF NEW.type = 'MONTHLY' THEN
            NEW.price := 50.00;
        ELSIF NEW.type = 'ANNUAL' THEN
            NEW.price := 500.00;
END IF;
END IF;

RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_set_subscription_end_date
    BEFORE INSERT ON subscriptions
    FOR EACH ROW EXECUTE FUNCTION set_subscription_end_date();

-- acest trigger verifica zilnic (sau la orice update) daca abonamentul
-- a expirat si ii schimba statusul automat in EXPIRED
CREATE OR REPLACE FUNCTION check_subscription_expiry()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.end_date < CURRENT_DATE AND NEW.status = 'ACTIVE' THEN
        NEW.status := 'EXPIRED';

        -- ridic o notificare (o vom prinde in Java ca warning, nu eroare)
        RAISE NOTICE 'SUBSCRIPTION_EXPIRED: Abonamentul % al pacientului % a expirat',
            NEW.id, NEW.patient_id;
END IF;

RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_check_subscription_expiry
    BEFORE UPDATE ON subscriptions
    FOR EACH ROW EXECUTE FUNCTION check_subscription_expiry();


-- trigger care blocheaza crearea unei consultatii daca pacientul nu are abonament activ.
-- Il definesc aici pentru ca depinde de tabela subscriptions,
-- dar va fi folosit pe tabela consultations (definita in V5)
-- IMPORTANT: triggerul efectiv pe consultations il adaug in V5, dar functia o creez aici ca sa fie disponibila
CREATE OR REPLACE FUNCTION check_active_subscription()
RETURNS TRIGGER AS $$
DECLARE
v_has_active BOOLEAN;
BEGIN
SELECT EXISTS (
    SELECT 1 FROM subscriptions
    WHERE patient_id = NEW.patient_id
      AND status = 'ACTIVE'
      AND end_date >= CURRENT_DATE
) INTO v_has_active;

IF NOT v_has_active THEN
        -- arunc o exceptie cu un cod pe care o s o prind in Java
        RAISE EXCEPTION 'NO_ACTIVE_SUBSCRIPTION: Pacientul % nu are un abonament activ',
            NEW.patient_id;
END IF;

RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- tin evidenta tuturor platilor pentru fiecare abonament.
-- chiar daca o plata esueaza, o inregistrez cu status = FAILED
-- ca sa am un istoric complet
CREATE TABLE payment_history (
                                 id                  BIGSERIAL PRIMARY KEY,
                                 subscription_id     BIGINT          NOT NULL REFERENCES subscriptions(id) ON DELETE CASCADE,
                                 amount              NUMERIC(10, 2)  NOT NULL,
                                 payment_date        TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                                 payment_method      VARCHAR(50)     NOT NULL DEFAULT 'CARD',
                                 status              payment_status  NOT NULL DEFAULT 'PENDING',
                                 transaction_id      VARCHAR(255)    UNIQUE,
                                 notes               TEXT,

                                 CONSTRAINT chk_amount          CHECK (amount > 0),
                                 CONSTRAINT chk_payment_method  CHECK (payment_method IN ('CARD', 'TRANSFER', 'CASH'))
);

CREATE INDEX idx_payment_history_subscription_id ON payment_history(subscription_id);
CREATE INDEX idx_payment_history_status          ON payment_history(status);

-- cand o plata este completata cu succes, activez automat abonamentul asociat
CREATE OR REPLACE FUNCTION activate_subscription_on_payment()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status = 'COMPLETED' AND OLD.status = 'PENDING' THEN
UPDATE subscriptions
SET status = 'ACTIVE'
WHERE id = NEW.subscription_id
  AND status = 'PENDING';
END IF;

RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_activate_subscription_on_payment
    AFTER UPDATE ON payment_history
    FOR EACH ROW EXECUTE FUNCTION activate_subscription_on_payment();