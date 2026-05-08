-- Reset secvente pentru seed data reproductibil
-- Reset dinamic al tuturor secventelor - sigur indiferent de naming
DO $$
DECLARE
seq RECORD;
BEGIN
FOR seq IN
SELECT sequence_name
FROM information_schema.sequences
WHERE sequence_schema = 'public'
    LOOP
        EXECUTE 'ALTER SEQUENCE public.' || seq.sequence_name || ' RESTART WITH 1';
END LOOP;
END $$;


-- Date de test realiste pentru demonstrarea functionalitatii sistemului.
-- Dezactivez triggerele pentru a putea insera date in stari finale
-- fara a parcurge intregul flux (ex: consultatii deja completate).
-- Le reactivez la final.

ALTER TABLE consultations        DISABLE TRIGGER ALL;
ALTER TABLE consultation_symptoms DISABLE TRIGGER ALL;
ALTER TABLE medical_form_answers  DISABLE TRIGGER ALL;
ALTER TABLE diagnoses             DISABLE TRIGGER ALL;
ALTER TABLE appointments          DISABLE TRIGGER ALL;
ALTER TABLE subscriptions         DISABLE TRIGGER ALL;
ALTER TABLE payment_history       DISABLE TRIGGER ALL;
ALTER TABLE patients              DISABLE TRIGGER ALL;
ALTER TABLE guardians             DISABLE TRIGGER ALL;

-- eliminam constrangerea de timp pentru appointments
-- ca sa putem insera programari in trecut (date istorice)
ALTER TABLE appointments DROP CONSTRAINT IF EXISTS chk_not_in_past;

-- ================================================================
-- USERS
-- ================================================================

INSERT INTO users (email, password_hash, role, is_active) VALUES
-- admini
('admin@telemedicina.ro',          '$2a$12$hash_admin1',   'ADMIN',   TRUE),
('superadmin@telemedicina.ro',     '$2a$12$hash_admin2',   'ADMIN',   TRUE),
-- doctori
('alex.ionescu@telemedicina.ro',   '$2a$12$hash_doc1',     'DOCTOR',  TRUE),
('maria.popescu@telemedicina.ro',  '$2a$12$hash_doc2',     'DOCTOR',  TRUE),
('andrei.dumitru@telemedicina.ro', '$2a$12$hash_doc3',     'DOCTOR',  TRUE),
('elena.constantin@telemedicina.ro','$2a$12$hash_doc4',    'DOCTOR',  TRUE),
('mihai.stanescu@telemedicina.ro', '$2a$12$hash_doc5',     'DOCTOR',  TRUE),
('cristina.munteanu@telemedicina.ro','$2a$12$hash_doc6',   'DOCTOR',  TRUE),
-- pacienti adulti
('ion.georgescu@gmail.com',        '$2a$12$hash_p1',       'PATIENT', TRUE),
('ana.popa@gmail.com',             '$2a$12$hash_p2',       'PATIENT', TRUE),
('vasile.marinescu@gmail.com',     '$2a$12$hash_p3',       'PATIENT', TRUE),
('maria.diaconu@gmail.com',        '$2a$12$hash_p4',       'PATIENT', TRUE),
('gheorghe.tudose@gmail.com',      '$2a$12$hash_p5',       'PATIENT', TRUE),
('elena.radulescu@gmail.com',      '$2a$12$hash_p6',       'PATIENT', TRUE),
('mihai.ciobanu@gmail.com',        '$2a$12$hash_p7',       'PATIENT', TRUE),
('ioana.filip@gmail.com',          '$2a$12$hash_p8',       'PATIENT', TRUE),
('constantin.vlad@gmail.com',      '$2a$12$hash_p9',       'PATIENT', TRUE),
('nicoleta.stan@gmail.com',        '$2a$12$hash_p10',      'PATIENT', TRUE),
-- parinti (tutori pentru copii)
('tudor.georgescu@gmail.com',      '$2a$12$hash_g1',       'PATIENT', TRUE),
('gabriela.popa@gmail.com',        '$2a$12$hash_g2',       'PATIENT', TRUE),
('catalin.ionescu@gmail.com',      '$2a$12$hash_g3',       'PATIENT', TRUE),
('diana.constantin@gmail.com',     '$2a$12$hash_g4',       'PATIENT', TRUE),
('florin.dumitru@gmail.com',       '$2a$12$hash_g5',       'PATIENT', TRUE);

-- ================================================================
-- PATIENTS (adulti - user_id 9-18, copii - user_id 19-23)
-- ================================================================

INSERT INTO patients (user_id, first_name, last_name, birth_date, age_category, gender, blood_type, phone, cnp, address) VALUES
                                                                                                                             (9,  'Ion',       'Georgescu',  '1979-03-15', 'ADULT',  'MALE',   'A+',  '0721111001', '1790315123456', 'Str. Mihai Viteazu 5, Bucuresti'),
                                                                                                                             (10, 'Ana',       'Popa',       '1992-07-22', 'ADULT',  'FEMALE', 'B+',  '0721111002', '2920722234567', 'Bd. Unirii 12, Bucuresti'),
                                                                                                                             (11, 'Vasile',    'Marinescu',  '1957-11-08', 'SENIOR', 'MALE',   'O+',  '0721111003', '1571108345678', 'Str. Libertatii 3, Cluj-Napoca'),
                                                                                                                             (12, 'Maria',     'Diaconu',    '1996-01-30', 'ADULT',  'FEMALE', 'AB-', '0721111004', '2960130456789', 'Str. Eminescu 8, Iasi'),
                                                                                                                             (13, 'Gheorghe',  'Tudose',     '1952-06-14', 'SENIOR', 'MALE',   'A-',  '0721111005', '1520614567890', 'Bd. Republicii 22, Brasov'),
                                                                                                                             (14, 'Elena',     'Radulescu',  '1973-09-25', 'ADULT',  'FEMALE', 'B-',  '0721111006', '2730925678901', 'Str. Stefan cel Mare 17, Constanta'),
                                                                                                                             (15, 'Mihai',     'Ciobanu',    '1986-04-11', 'ADULT',  'MALE',   'O-',  '0721111007', '1860411789012', 'Calea Victoriei 44, Bucuresti'),
                                                                                                                             (16, 'Ioana',     'Filip',      '1980-12-03', 'ADULT',  'FEMALE', 'A+',  '0721111008', '2801203890123', 'Str. Pacii 9, Timisoara'),
                                                                                                                             (17, 'Constantin','Vlad',       '1965-08-19', 'ADULT',  'MALE',   'B+',  '0721111009', '1650819901234', 'Str. Revolutiei 6, Craiova'),
                                                                                                                             (18, 'Nicoleta',  'Stan',       '1989-02-28', 'ADULT',  'FEMALE', 'O+',  '0721111010', '2890228012345', 'Bd. Decebal 31, Pitesti'),
-- copii (tutori: user_id 19-23)
                                                                                                                             (19, 'Andrei',    'Georgescu',  '2016-05-10', 'CHILD',  'MALE',   'A+',  NULL, NULL, 'Str. Mihai Viteazu 5, Bucuresti'),
                                                                                                                             (20, 'Sofia',     'Popa',       '2019-09-03', 'CHILD',  'FEMALE', 'B+',  NULL, NULL, 'Bd. Libertatii 7, Bucuresti'),
                                                                                                                             (21, 'Rares',     'Ionescu',    '2012-11-21', 'CHILD',  'MALE',   'O+',  NULL, NULL, 'Str. Florilor 2, Cluj-Napoca'),
                                                                                                                             (22, 'Emma',      'Constantin', '2021-03-15', 'CHILD',  'FEMALE', 'AB+', NULL, NULL, 'Str. Trandafirilor 4, Iasi'),
                                                                                                                             (23, 'Luca',      'Dumitru',    '2014-07-08', 'CHILD',  'MALE',   'A-',  NULL, NULL, 'Calea Mosilor 18, Bucuresti');

-- ================================================================
-- GUARDIANS
-- ================================================================

INSERT INTO guardians (patient_id, guardian_user_id, first_name, last_name, phone, email, relationship) VALUES
                                                                                                            (11, 19, 'Tudor',    'Georgescu',  '0722222001', 'tudor.georgescu@gmail.com',   'PARENT'),
                                                                                                            (12, 20, 'Gabriela', 'Popa',       '0722222002', 'gabriela.popa@gmail.com',     'PARENT'),
                                                                                                            (13, 21, 'Catalin',  'Ionescu',    '0722222003', 'catalin.ionescu@gmail.com',   'PARENT'),
                                                                                                            (14, 22, 'Diana',    'Constantin', '0722222004', 'diana.constantin@gmail.com',  'PARENT'),
                                                                                                            (15, 23, 'Florin',   'Dumitru',    '0722222005', 'florin.dumitru@gmail.com',    'PARENT');

-- ================================================================
-- CHRONIC CONDITIONS
-- ================================================================

INSERT INTO chronic_conditions (patient_id, condition_name, diagnosed_date, severity, is_active, notes) VALUES
                                                                                                            (1,  'Hipertensiune arteriala',        '2018-03-10', 'MODERATE', TRUE,  'Tratament cu Enalapril 10mg'),
                                                                                                            (1,  'Diabet zaharat tip 2',           '2020-07-15', 'MODERATE', TRUE,  'Metformin 1000mg, monitorizare glicemie'),
                                                                                                            (3,  'Insuficienta cardiaca cronica',  '2015-11-20', 'SEVERE',   TRUE,  'Sub monitorizare cardiologica permanenta'),
                                                                                                            (3,  'Diabet zaharat tip 2',           '2012-04-08', 'SEVERE',   TRUE,  'Insulinoterapie'),
                                                                                                            (5,  'Hipertensiune arteriala',        '2010-09-12', 'SEVERE',   TRUE,  'Tratament combinat antihipertensiv'),
                                                                                                            (5,  'Artrita reumatoida',             '2014-02-28', 'MODERATE', TRUE,  'Reumatologie - tratament biologic'),
                                                                                                            (6,  'Gastrita cronica',               '2019-06-05', 'MILD',     TRUE,  'Regim alimentar, Omeprazol la nevoie'),
                                                                                                            (6,  'Alergie la penicilina',          '2005-01-01', 'SEVERE',   TRUE,  'ATENTIE: alergie severa confirmata'),
                                                                                                            (7,  'Astm bronsic',                   '2016-08-14', 'MODERATE', TRUE,  'Ventolin la nevoie, Flixotide preventiv'),
                                                                                                            (8,  'Sindrom de colon iritabil',      '2021-03-22', 'MILD',     TRUE,  'Regim alimentar specific'),
                                                                                                            (9,  'Hipertensiune arteriala',        '2022-01-10', 'MILD',     TRUE,  'Sub tratament, tensiune controlata'),
                                                                                                            (10, 'Hipotiroidism',                  '2020-11-30', 'MODERATE', TRUE,  'Eutirox 75mg zilnic'),
                                                                                                            (2,  'Alergie la ibuprofen',           '2015-05-15', 'MODERATE', TRUE,  'Evitati AINS. Alternativa: Paracetamol'),
                                                                                                            (4,  'Migrena cronica',                '2018-09-01', 'MODERATE', TRUE,  'Tratament abortiv si profilactic'),
                                                                                                            (13, 'Astm bronsic infantil',          '2017-04-20', 'MILD',     TRUE,  'Ventolin la nevoie');

-- ================================================================
-- DOCTORS
-- ================================================================

INSERT INTO doctors (user_id, first_name, last_name, specialization, license_number, phone, bio) VALUES
                                                                                                     (3, 'Alexandru', 'Ionescu',   'Medicina Interna',   'CMR-2010-001', '0733333001', 'Specialist medicina interna, 15 ani experienta'),
                                                                                                     (4, 'Maria',     'Popescu',   'Medicina de Familie', 'CMR-2008-002', '0733333002', 'Medic de familie, abordare holistica'),
                                                                                                     (5, 'Andrei',    'Dumitru',   'Pediatrie',           'CMR-2012-003', '0733333003', 'Pediatru cu experienta in boli infectioase pediatrice'),
                                                                                                     (6, 'Elena',     'Constantin','Medicina Interna',   'CMR-2009-004', '0733333004', 'Subspecializare in boli digestive'),
                                                                                                     (7, 'Mihai',     'Stanescu',  'Medicina de Familie', 'CMR-2015-005', '0733333005', 'Medic de familie, telemedicina din 2020'),
                                                                                                     (8, 'Cristina',  'Munteanu',  'Pediatrie',           'CMR-2014-006', '0733333006', 'Pediatru, urgente pediatrice');

-- ================================================================
-- DOCTOR SCHEDULES
-- Zilele: 0=Luni, 1=Marti, 2=Miercuri, 3=Joi, 4=Vineri, 5=Sambata, 6=Duminica
-- ================================================================

INSERT INTO doctor_schedules (doctor_id, day_of_week, start_time, end_time, is_active) VALUES
-- Dr. Ionescu: Luni-Vineri 08:00-13:00
(1, 0, '08:00', '13:00', TRUE),
(1, 1, '08:00', '13:00', TRUE),
(1, 2, '08:00', '13:00', TRUE),
(1, 3, '08:00', '13:00', TRUE),
(1, 4, '08:00', '13:00', TRUE),
-- Dr. Popescu: Luni-Vineri 11:00-17:00
(2, 0, '11:00', '17:00', TRUE),
(2, 1, '11:00', '17:00', TRUE),
(2, 2, '11:00', '17:00', TRUE),
(2, 3, '11:00', '17:00', TRUE),
(2, 4, '11:00', '17:00', TRUE),
-- Dr. Dumitru (pediatru): Luni-Vineri 14:00-19:00
(3, 0, '14:00', '19:00', TRUE),
(3, 1, '14:00', '19:00', TRUE),
(3, 2, '14:00', '19:00', TRUE),
(3, 3, '14:00', '19:00', TRUE),
(3, 4, '14:00', '19:00', TRUE),
-- Dr. Constantin: Luni-Vineri 08:00-15:00
(4, 0, '08:00', '15:00', TRUE),
(4, 1, '08:00', '15:00', TRUE),
(4, 2, '08:00', '15:00', TRUE),
(4, 3, '08:00', '15:00', TRUE),
(4, 4, '08:00', '15:00', TRUE),
-- Dr. Stanescu: Marti-Sambata 09:00-16:00
(5, 1, '09:00', '16:00', TRUE),
(5, 2, '09:00', '16:00', TRUE),
(5, 3, '09:00', '16:00', TRUE),
(5, 4, '09:00', '16:00', TRUE),
(5, 5, '09:00', '16:00', TRUE),
-- Dr. Munteanu (pediatru): Luni-Vineri 10:00-18:00
(6, 0, '10:00', '18:00', TRUE),
(6, 1, '10:00', '18:00', TRUE),
(6, 2, '10:00', '18:00', TRUE),
(6, 3, '10:00', '18:00', TRUE),
(6, 4, '10:00', '18:00', TRUE);

-- ================================================================
-- SUBSCRIPTIONS
-- ================================================================

INSERT INTO subscriptions (patient_id, type, start_date, end_date, status, price) VALUES
                                                                                      (1,  'ANNUAL',  '2025-01-01', '2026-01-01', 'ACTIVE',   500.00),
                                                                                      (2,  'MONTHLY', '2025-04-01', '2025-05-01', 'ACTIVE',    50.00),
                                                                                      (3,  'ANNUAL',  '2024-06-01', '2025-06-01', 'ACTIVE',   500.00),
                                                                                      (4,  'MONTHLY', '2025-03-15', '2025-04-15', 'EXPIRED',   50.00),
                                                                                      (5,  'ANNUAL',  '2024-09-01', '2025-09-01', 'ACTIVE',   500.00),
                                                                                      (6,  'MONTHLY', '2025-04-10', '2025-05-10', 'ACTIVE',    50.00),
                                                                                      (7,  'ANNUAL',  '2025-02-01', '2026-02-01', 'ACTIVE',   500.00),
                                                                                      (8,  'MONTHLY', '2025-04-20', '2025-05-20', 'ACTIVE',    50.00),
                                                                                      (9,  'ANNUAL',  '2024-12-01', '2025-12-01', 'ACTIVE',   500.00),
                                                                                      (10, 'MONTHLY', '2025-04-05', '2025-05-05', 'ACTIVE',    50.00),
                                                                                      (11, 'ANNUAL',  '2025-01-15', '2026-01-15', 'ACTIVE',   500.00),
                                                                                      (12, 'MONTHLY', '2025-03-01', '2025-04-01', 'EXPIRED',   50.00),
                                                                                      (13, 'ANNUAL',  '2024-08-01', '2025-08-01', 'ACTIVE',   500.00),
                                                                                      (14, 'MONTHLY', '2025-04-25', '2025-05-25', 'ACTIVE',    50.00),
                                                                                      (15, 'ANNUAL',  '2025-03-01', '2026-03-01', 'ACTIVE',   500.00);

-- ================================================================
-- PAYMENT HISTORY
-- ================================================================

INSERT INTO payment_history (subscription_id, amount, payment_date, payment_method, status, transaction_id) VALUES
                                                                                                                (1,  500.00, '2025-01-01 10:00:00+02', 'CARD',     'COMPLETED', 'TXN-2025-001'),
                                                                                                                (2,   50.00, '2025-04-01 09:30:00+02', 'CARD',     'COMPLETED', 'TXN-2025-002'),
                                                                                                                (3,  500.00, '2024-06-01 11:00:00+02', 'TRANSFER', 'COMPLETED', 'TXN-2024-003'),
                                                                                                                (4,   50.00, '2025-03-15 14:00:00+02', 'CARD',     'COMPLETED', 'TXN-2025-004'),
                                                                                                                (5,  500.00, '2024-09-01 08:30:00+02', 'CARD',     'COMPLETED', 'TXN-2024-005'),
                                                                                                                (6,   50.00, '2025-04-10 16:00:00+02', 'CARD',     'COMPLETED', 'TXN-2025-006'),
                                                                                                                (7,  500.00, '2025-02-01 10:00:00+02', 'TRANSFER', 'COMPLETED', 'TXN-2025-007'),
                                                                                                                (8,   50.00, '2025-04-20 09:00:00+02', 'CARD',     'COMPLETED', 'TXN-2025-008'),
                                                                                                                (9,  500.00, '2024-12-01 11:30:00+02', 'CARD',     'COMPLETED', 'TXN-2024-009'),
                                                                                                                (10,  50.00, '2025-04-05 13:00:00+02', 'CARD',     'COMPLETED', 'TXN-2025-010'),
                                                                                                                (11, 500.00, '2025-01-15 10:00:00+02', 'TRANSFER', 'COMPLETED', 'TXN-2025-011'),
                                                                                                                (12,  50.00, '2025-03-01 09:00:00+02', 'CARD',     'COMPLETED', 'TXN-2025-012'),
                                                                                                                (13, 500.00, '2024-08-01 10:00:00+02', 'CARD',     'COMPLETED', 'TXN-2024-013'),
                                                                                                                (14,  50.00, '2025-04-25 14:30:00+02', 'CARD',     'COMPLETED', 'TXN-2025-014'),
                                                                                                                (15, 500.00, '2025-03-01 11:00:00+02', 'TRANSFER', 'COMPLETED', 'TXN-2025-015'),
-- o plata esuata - pentru testarea exceptiei din Java
                                                                                                                (2,   50.00, '2025-03-01 08:00:00+02', 'CARD',     'FAILED',    'TXN-2025-FAIL');

-- ================================================================
-- CONSULTATIONS
-- ================================================================

INSERT INTO consultations (patient_id, status, complexity_level, emergency_redirect, notes) VALUES
                                                                                                (1,  'COMPLETED',          'SIMPLE',    FALSE, 'Viroza respiratorie - rezolvata cu reteta automata'),
                                                                                                (2,  'COMPLETED',          'MEDIUM',    FALSE, 'Gripa - consultatie cu Dr. Popescu'),
                                                                                                (3,  'COMPLETED',          'COMPLEX',   FALSE, 'Pacient cu multiple comorbiditati'),
                                                                                                (5,  'COMPLETED',          'MEDIUM',    FALSE, 'Toxiinfectie alimentara'),
                                                                                                (6,  'SCHEDULED',          'MEDIUM',    FALSE, 'Programata pentru maine'),
                                                                                                (7,  'DIAGNOSIS_PENDING',  'SIMPLE',    FALSE, 'Asteapta decizia pacientului'),
                                                                                                (8,  'FORM_COMPLETED',     'MEDIUM',    FALSE, 'Fisa completata, in asteptare diagnostic'),
                                                                                                (9,  'COMPLETED',          'SIMPLE',    FALSE, 'Viroza - reteta automata emisa'),
                                                                                                (10, 'CANCELLED',          NULL,        FALSE, 'Anulata de pacient'),
                                                                                                (1,  'COMPLETED',          'MEDIUM',    FALSE, 'A doua consultatie - gripa'),
                                                                                                (4,  'COMPLETED',          'SIMPLE',    FALSE, 'Viroza - reteta automata'),
                                                                                                (2,  'SCHEDULED',          'COMPLEX',   FALSE, 'Caz complex - programata la Dr. Ionescu'),
                                                                                                (11, 'COMPLETED',          'EMERGENCY', TRUE,  'Redirectionat la UPU - posibila apendicita'),
                                                                                                (15, 'COMPLETED',          'MEDIUM',    FALSE, 'Copil - otita - consultatie pediatru'),
                                                                                                (6,  'FORM_GENERATED',     'SIMPLE',    FALSE, 'Fisa generata, asteapta completare');

-- ================================================================
-- CONSULTATION SYMPTOMS
-- ================================================================

INSERT INTO consultation_symptoms (consultation_id, symptom_name, severity, order_index) VALUES
-- consultatie 1: viroza simpla
(1, 'Febra',              'MILD',     1),
(1, 'Durere de cap',      'MILD',     2),
(1, 'Tuse seaca',         'MODERATE', 3),
-- consultatie 2: gripa
(2, 'Febra mare',         'SEVERE',   1),
(2, 'Dureri musculare',   'SEVERE',   2),
(2, 'Oboseala extrema',   'SEVERE',   3),
-- consultatie 3: complex, comorbiditati
(3, 'Dificultati respiratorii', 'SEVERE',   1),
(3, 'Durere in piept',    'MODERATE', 2),
(3, 'Oboseala',           'SEVERE',   3),
-- consultatie 4: toxiinfectie
(4, 'Varsaturi',          'SEVERE',   1),
(4, 'Dureri abdominale',  'MODERATE', 2),
(4, 'Febra',              'MODERATE', 3),
-- consultatie 5: in asteptare
(5, 'Febra',              'MODERATE', 1),
(5, 'Varsaturi',          'MODERATE', 2),
(5, 'Dureri abdominale',  'MILD',     3),
-- consultatie 6
(6, 'Durere de cap',      'MODERATE', 1),
(6, 'Febra',              'MILD',     2),
(6, 'Tuse',               'MILD',     3),
-- consultatie 7
(7, 'Febra',              'MODERATE', 1),
(7, 'Dureri musculare',   'MODERATE', 2),
(7, 'Durere de cap',      'MODERATE', 3),
-- consultatie 8
(8, 'Tuse productiva',    'MODERATE', 1),
(8, 'Durere in piept',    'MODERATE', 2),
(8, 'Febra',              'MILD',     3),
-- consultatie 9: viroza simpla
(9, 'Nas infundat',       'MILD',     1),
(9, 'Durere de gat',      'MILD',     2),
(9, 'Febra mica',         'MILD',     3),
-- consultatie 10: anulata
(10, 'Oboseala',          'MILD',     1),
(10, 'Durere de cap',     'MILD',     2),
(10, 'Febra',             'MILD',     3),
-- consultatie 11: a doua consultatie pac 1
(11, 'Febra mare',        'SEVERE',   1),
(11, 'Dureri musculare',  'SEVERE',   2),
(11, 'Frisoane',          'MODERATE', 3),
-- consultatie 12: complex
(12, 'Durere in piept',   'SEVERE',   1),
(12, 'Dificultati respiratorii', 'SEVERE', 2),
(12, 'Oboseala severa',   'SEVERE',   3),
-- consultatie 13: urgenta - apendicita
(13, 'Durere abdominala acuta', 'SEVERE', 1),
(13, 'Varsaturi',         'SEVERE',   2),
(13, 'Febra',             'MODERATE', 3),
-- consultatie 14: copil otita
(14, 'Febra',             'MODERATE', 1),
(14, 'Durere ureche',     'SEVERE',   2),
(14, 'Lipsa pofta mancare', 'MODERATE', 3),
-- consultatie 15: in generare
(15, 'Febra',             'MILD',     1),
(15, 'Tuse',              'MILD',     2),
(15, 'Durere de gat',     'MILD',     3);

-- ================================================================
-- MEDICAL FORM QUESTIONS (selectie reprezentativa)
-- ================================================================

INSERT INTO medical_form_questions
(consultation_id, question_text, question_type, options, order_index, is_required)
VALUES
-- consultatie 1
(1, 'De cate zile aveti aceste simptome?',
 'MULTIPLE_CHOICE', '["1 zi", "2-3 zile", "4-7 zile", "Peste o saptamana"]', 1, TRUE),
(1, 'Cum ati evalua intensitatea simptomelor?',
 'MULTIPLE_CHOICE', '["Usoare", "Moderate", "Severe"]', 2, TRUE),
(1, 'Ce temperatura aveti?',
 'MULTIPLE_CHOICE', '["37-37.5°C", "37.5-38.5°C", "38.5-39.5°C", "Peste 39.5°C"]', 3, TRUE),
(1, 'Aveti dureri musculare sau articulare?', 'YES_NO', NULL, 4, TRUE),
(1, 'Aveti dureri in gat sau dificultate la inghitire?', 'YES_NO', NULL, 5, TRUE),
-- consultatie 2
(2, 'De cate zile aveti aceste simptome?',
 'MULTIPLE_CHOICE', '["1 zi", "2-3 zile", "4-7 zile", "Peste o saptamana"]', 1, TRUE),
(2, 'Ce temperatura aveti?',
 'MULTIPLE_CHOICE', '["37-37.5°C", "37.5-38.5°C", "38.5-39.5°C", "Peste 39.5°C"]', 2, TRUE),
(2, 'Aveti dureri musculare sau articulare?', 'YES_NO', NULL, 3, TRUE),
(2, 'Mai sunt si alte persoane din anturaj cu aceleasi simptome?', 'YES_NO', NULL, 4, TRUE),
(2, 'Ati fost vaccinat antigripal in acest sezon?', 'YES_NO', NULL, 5, FALSE),
-- consultatie 4 (toxiinfectie)
(4, 'De cate zile aveti aceste simptome?',
 'MULTIPLE_CHOICE', '["1 zi", "2-3 zile", "4-7 zile", "Peste o saptamana"]', 1, TRUE),
(4, 'Ati consumat alimente posibil alterate in ultimele 24 ore?', 'YES_NO', NULL, 2, TRUE),
(4, 'Aveti diaree?', 'YES_NO', NULL, 3, TRUE),
(4, 'Mai sunt si alte persoane din anturaj cu aceleasi simptome?', 'YES_NO', NULL, 4, TRUE),
(4, 'Varsaturile sunt frecvente (mai mult de 3 episoade pe zi)?', 'YES_NO', NULL, 5, FALSE),
-- consultatie 13 (urgenta - apendicita)
(13, 'Durerea este localizata in partea dreapta jos a abdomenului?', 'YES_NO', NULL, 1, TRUE),
(13, 'De cat timp au inceput simptomele?',
 'MULTIPLE_CHOICE', '["Sub 1 ora", "1-6 ore", "6-12 ore", "Peste 12 ore"]', 2, TRUE),
(13, 'Durerea s-a intensificat progresiv de la debut?', 'YES_NO', NULL, 3, TRUE),
(13, 'Aveti febra?', 'YES_NO', NULL, 4, TRUE),
-- consultatie 14 (copil otita)
(14, 'Copilul are eruptii cutanate sau pete pe piele?', 'YES_NO', NULL, 1, TRUE),
(14, 'Copilul isi trage de urechi sau se plange de dureri in urechi?', 'YES_NO', NULL, 2, TRUE),
(14, 'Ce temperatura are copilul?',
 'MULTIPLE_CHOICE', '["37-37.5°C", "37.5-38.5°C", "38.5-39.5°C", "Peste 39.5°C"]', 3, TRUE),
(14, 'Copilul a fost in contact cu alti copii bolnavi in ultimele 7 zile?', 'YES_NO', NULL, 4, TRUE),
(14, 'Copilul are dificultati de respiratie?', 'YES_NO', NULL, 5, TRUE);

-- ================================================================
-- MEDICAL FORM ANSWERS
-- ================================================================

INSERT INTO medical_form_answers (question_id, consultation_id, answer_text) VALUES
-- raspunsuri consultatie 1 (viroza)
(1,  1, '2-3 zile'),
(2,  1, 'Usoare'),
(3,  1, '37-37.5°C (subfebrilitate)'),
(4,  1, 'Nu'),
(5,  1, 'Da'),
-- raspunsuri consultatie 2 (gripa)
(6,  2, '2-3 zile'),
(7,  2, 'Peste 39.5°C'),
(8,  2, 'Da'),
(9,  2, 'Da'),
(10, 2, 'Nu'),
-- raspunsuri consultatie 4 (toxiinfectie)
(11, 4, '1 zi'),
(12, 4, 'Da'),
(13, 4, 'Da'),
(14, 4, 'Nu'),
(15, 4, 'Da'),
-- raspunsuri consultatie 13 (urgenta)
(16, 13, 'Da'),
(17, 13, '1-6 ore'),
(18, 13, 'Da'),
(19, 13, 'Da'),
-- raspunsuri consultatie 14 (copil otita)
(20, 14, 'Nu'),
(21, 14, 'Da'),
(22, 14, '38.5-39.5°C (febra mare)'),
(23, 14, 'Da'),
(24, 14, 'Nu');

-- ================================================================
-- DIAGNOSES
-- ================================================================

INSERT INTO diagnoses
(consultation_id, diagnosis_name, diagnosis_type, complexity_level, icd_code, confidence_score, notes, created_by)
VALUES
-- consultatie 1: viroza (auto + confirmat)
(1, 'Viroza respiratorie',           'AUTO_GENERATED', 'SIMPLE',    'J06', 78, NULL,                              NULL),
(1, 'Viroza respiratorie',           'CONFIRMED',      'SIMPLE',    'J06', 95, 'Confirmat, reteta automata emisa', 3),
-- consultatie 2: gripa (auto + confirmat)
(2, 'Gripa',                         'AUTO_GENERATED', 'MEDIUM',    'J10', 82, NULL,                              NULL),
(2, 'Gripa',                         'CONFIRMED',      'MEDIUM',    'J10', 95, 'Confirmat de Dr. Popescu',         4),
-- consultatie 3: complex
(3, 'Insuficienta cardiaca acutizata','AUTO_GENERATED', 'COMPLEX',   'I50', 55, 'Scor de incredere scazut - caz complex', NULL),
(3, 'Bronsita acuta',                 'AUTO_GENERATED', 'COMPLEX',   'J20', 40, NULL,                              NULL),
(3, 'Insuficienta cardiaca acutizata','CONFIRMED',      'COMPLEX',   'I50', 90, 'Confirmat, pacient internat',      3),
-- consultatie 4: toxiinfectie
(4, 'Toxiinfectie alimentara',        'AUTO_GENERATED', 'MEDIUM',    'A05', 88, NULL,                              NULL),
(4, 'Toxiinfectie alimentara',        'CONFIRMED',      'MEDIUM',    'A05', 95, 'Confirmat de Dr. Constantin',      6),
-- consultatie 9: viroza simpla
(9, 'Viroza respiratorie',            'AUTO_GENERATED', 'SIMPLE',    'J06', 75, NULL,                              NULL),
(9, 'Viroza respiratorie',            'CONFIRMED',      'SIMPLE',    'J06', 90, 'Reteta automata confirmata',       5),
-- consultatie 11: gripa
(11,'Gripa',                          'AUTO_GENERATED', 'MEDIUM',    'J10', 85, NULL,                              NULL),
(11,'Gripa',                          'CONFIRMED',      'MEDIUM',    'J10', 92, 'Confirmat de Dr. Ionescu',         3),
-- consultatie 13: urgenta apendicita
(13,'Posibila apendicita acuta - necesita evaluare chirurgicala de urgenta',
 'AUTO_GENERATED', 'EMERGENCY', 'K37', 75, 'Redirectionat catre UPU',       NULL),
-- consultatie 14: copil otita
(14,'Otita medie acuta',              'AUTO_GENERATED', 'MEDIUM',    'H66', 80, NULL,                              NULL),
(14,'Otita medie acuta',              'CONFIRMED',      'MEDIUM',    'H66', 95, 'Confirmat de Dr. Dumitru',         5);

-- ================================================================
-- APPOINTMENTS
-- ================================================================

INSERT INTO appointments
(consultation_id, doctor_id, patient_id, start_time, end_time, duration_minutes, status, notes)
VALUES
    (2,  2, 2,  '2025-04-10 11:00:00+02', '2025-04-10 11:20:00+02', 20, 'COMPLETED', 'Gripa - consultatie finalizata'),
    (3,  1, 3,  '2025-04-08 08:00:00+02', '2025-04-08 08:30:00+02', 30, 'COMPLETED', 'Caz complex - internat dupa consultatie'),
    (4,  4, 4,  '2025-04-05 09:00:00+02', '2025-04-05 09:20:00+02', 20, 'COMPLETED', 'Toxiinfectie - tratament la domiciliu'),
    (5,  2, 5,  '2025-05-06 14:00:00+02', '2025-05-06 14:20:00+02', 20, 'SCHEDULED', 'Programare pentru maine'),
    (11, 1, 1,  '2025-04-15 08:30:00+02', '2025-04-15 08:50:00+02', 20, 'COMPLETED', 'A doua consultatie - gripa'),
    (12, 1, 2,  '2025-05-08 09:00:00+02', '2025-05-08 09:30:00+02', 30, 'SCHEDULED', 'Caz complex programat'),
    (14, 3, 14, '2025-04-20 14:00:00+02', '2025-04-20 14:20:00+02', 20, 'COMPLETED', 'Otita copil - tratament prescris');

-- ================================================================
-- PRESCRIPTIONS
-- ================================================================

INSERT INTO prescriptions
(consultation_id, patient_id, diagnosis_id, issued_at, valid_until, is_auto_generated)
VALUES
    (1,  1,  2,  '2025-04-02 10:30:00+02', '2025-04-09 10:30:00+02', TRUE),
    (2,  2,  4,  '2025-04-10 11:25:00+02', '2025-04-17 11:25:00+02', FALSE),
    (4,  4,  9,  '2025-04-05 09:25:00+02', '2025-04-12 09:25:00+02', FALSE),
    (9,  9,  11, '2025-04-18 15:00:00+02', '2025-04-25 15:00:00+02', TRUE),
    (11, 1,  13, '2025-04-15 08:55:00+02', '2025-04-22 08:55:00+02', FALSE),
    (14, 14, 16, '2025-04-20 14:25:00+02', '2025-04-30 14:25:00+02', FALSE);

-- ================================================================
-- PRESCRIPTION MEDICATIONS
-- ================================================================

INSERT INTO prescription_medications
(prescription_id, medication_name, dosage, frequency, duration_days, instructions)
VALUES
-- reteta 1 (viroza automata)
(1, 'Paracetamol 500mg',      '1 comprimat', 'La 6-8 ore, la nevoie', 5,
 'Nu depasiti 4 comprimate pe zi.'),
(1, 'Vitamina C 1000mg',      '1 comprimat efervescent', 'O data pe zi', 7,
 'Dizolvati in apa. Administrati dupa masa.'),
(1, 'Ibuprofen 400mg',        '1 comprimat', 'La 8 ore, la nevoie', 3,
 'Administrati dupa masa. Contraindicat in ulcer gastric.'),
-- reteta 2 (gripa - doctor)
(2, 'Paracetamol 1000mg',     '1 comprimat', 'La 8 ore', 5,
 'Nu depasiti 3 comprimate pe zi.'),
(2, 'Vitamina C 1000mg',      '1 comprimat', 'O data pe zi', 10,
 'Dupa masa.'),
(2, 'Spray nazal cu apa de mare', '2 pufuri pe nara', 'De 4 ori pe zi', 7,
 'Inainte de fiecare masa.'),
-- reteta 3 (toxiinfectie)
(3, 'Smecta (Diosmectita)',   '1 plic', 'De 3 ori pe zi', 3,
 'Dizolvati in 100ml apa. Intre mese.'),
(3, 'Solutie rehidratanta orala', '200ml', 'Dupa fiecare episod', 3,
 'Inghitituri mici. Essential pentru rehidratare.'),
(3, 'Paracetamol 500mg',      '1 comprimat', 'La 6-8 ore, la nevoie', 3,
 'Doar daca aveti febra.'),
-- reteta 4 (viroza automata pac 9)
(4, 'Paracetamol 500mg',      '1 comprimat', 'La 6-8 ore, la nevoie', 5,
 'Nu depasiti 4 comprimate pe zi.'),
(4, 'Vitamina C 1000mg',      '1 comprimat efervescent', 'O data pe zi', 7,
 'Dupa masa.'),
-- reteta 6 (otita copil)
(6, 'Paracetamol sirop 240mg/5ml', '5ml',  'La 6-8 ore, la nevoie', 5,
 'Doz')