-- Reset secvente
DO $$
DECLARE seq RECORD;
BEGIN
FOR seq IN SELECT sequence_name FROM information_schema.sequences WHERE sequence_schema = 'public'
    LOOP
        EXECUTE 'ALTER SEQUENCE public.' || seq.sequence_name || ' RESTART WITH 1';
END LOOP;
END $$;

ALTER TABLE consultations         DISABLE TRIGGER ALL;
ALTER TABLE consultation_symptoms DISABLE TRIGGER ALL;
ALTER TABLE medical_form_answers  DISABLE TRIGGER ALL;
ALTER TABLE diagnoses             DISABLE TRIGGER ALL;
ALTER TABLE appointments          DISABLE TRIGGER ALL;
ALTER TABLE subscriptions         DISABLE TRIGGER ALL;
ALTER TABLE payment_history       DISABLE TRIGGER ALL;
ALTER TABLE patients              DISABLE TRIGGER ALL;
ALTER TABLE guardians             DISABLE TRIGGER ALL;

ALTER TABLE appointments DROP CONSTRAINT IF EXISTS chk_not_in_past;

INSERT INTO users (email, password_hash, role, is_active) VALUES
-- admins (id 1-2)
('admin@telemedicina.ro',             '$2a$12$hash_admin1',  'ADMIN',   TRUE),
('superadmin@telemedicina.ro',        '$2a$12$hash_admin2',  'ADMIN',   TRUE),
-- doctori generali (id 3-8)
('alex.ionescu@telemedicina.ro',      '$2a$12$hash_doc1',    'DOCTOR',  TRUE),
('maria.popescu@telemedicina.ro',     '$2a$12$hash_doc2',    'DOCTOR',  TRUE),
('andrei.dumitru@telemedicina.ro',    '$2a$12$hash_doc3',    'DOCTOR',  TRUE),
('elena.constantin@telemedicina.ro',  '$2a$12$hash_doc4',    'DOCTOR',  TRUE),
('mihai.stanescu@telemedicina.ro',    '$2a$12$hash_doc5',    'DOCTOR',  TRUE),
('cristina.munteanu@telemedicina.ro', '$2a$12$hash_doc6',    'DOCTOR',  TRUE),
-- pacienti adulti (id 9-18)
('ion.georgescu@gmail.com',           '$2a$12$hash_p1',      'PATIENT', TRUE),
('ana.popa@gmail.com',                '$2a$12$hash_p2',      'PATIENT', TRUE),
('vasile.marinescu@gmail.com',        '$2a$12$hash_p3',      'PATIENT', TRUE),
('maria.diaconu@gmail.com',           '$2a$12$hash_p4',      'PATIENT', TRUE),
('gheorghe.tudose@gmail.com',         '$2a$12$hash_p5',      'PATIENT', TRUE),
('elena.radulescu@gmail.com',         '$2a$12$hash_p6',      'PATIENT', TRUE),
('mihai.ciobanu@gmail.com',           '$2a$12$hash_p7',      'PATIENT', TRUE),
('ioana.filip@gmail.com',             '$2a$12$hash_p8',      'PATIENT', TRUE),
('constantin.vlad@gmail.com',         '$2a$12$hash_p9',      'PATIENT', TRUE),
('nicoleta.stan@gmail.com',           '$2a$12$hash_p10',     'PATIENT', TRUE),
-- parinti tutori (id 19-23)
('tudor.georgescu@gmail.com',         '$2a$12$hash_g1',      'PATIENT', TRUE),
('gabriela.popa@gmail.com',           '$2a$12$hash_g2',      'PATIENT', TRUE),
('catalin.ionescu@gmail.com',         '$2a$12$hash_g3',      'PATIENT', TRUE),
('diana.constantin@gmail.com',        '$2a$12$hash_g4',      'PATIENT', TRUE),
('florin.dumitru@gmail.com',          '$2a$12$hash_g5',      'PATIENT', TRUE),
-- doctori specialisti (id 24-27)
('ion.radulescu@telemedicina.ro',     '$2a$12$hash_doc7',    'DOCTOR',  TRUE),
('ana.florescu@telemedicina.ro',      '$2a$12$hash_doc8',    'DOCTOR',  TRUE),
('george.popa@telemedicina.ro',       '$2a$12$hash_doc9',    'DOCTOR',  TRUE),
('ioana.neagu@telemedicina.ro',       '$2a$12$hash_doc10',   'DOCTOR',  TRUE);


INSERT INTO patients (user_id, first_name, last_name, birth_date, age_category, gender, blood_type, phone, cnp, address) VALUES
                                                                                                                             (9,  'Ion',       'Georgescu',  '1979-03-15', 'ADULT',  'MALE',   'A+',  '0721111001', '1790315123456', 'Str. Mihai Viteazu 5, Bucuresti'),

INSERT INTO guardians (patient_id, guardian_user_id, first_name, last_name, phone, email, relationship) VALUES
                                                                                                            (11, 19, 'Tudor',    'Georgescu',  '0722222001', 'tudor.georgescu@gmail.com',  'PARENT'),

INSERT INTO chronic_conditions (patient_id, condition_name, diagnosed_date, severity, is_active, notes) VALUES
                                                                                                            (1,  'Hypertension',               '2018-03-10', 'MODERATE', TRUE,  'Treatment: Enalapril 10mg'),

INSERT INTO doctors (user_id, first_name, last_name, specialization, license_number, phone, bio, is_available) VALUES
                                                                                                                   (3,  'Alexandru', 'Ionescu',   'Medicina Interna',    'CMR-2010-001', '0733333001', 'Specialist medicina interna, 15 ani experienta. Preia cazuri generale adulti.',        TRUE),

INSERT INTO doctor_schedules (doctor_id, day_of_week, start_time, end_time, is_active) VALUES
-- Dr. Ionescu (Medicina Interna) — Luni-Vineri 08:00-13:00
(1,0,'08:00','13:00',TRUE),(1,1,'08:00','13:00',TRUE),(1,2,'08:00','13:00',TRUE),(1,3,'08:00','13:00',TRUE),(1,4,'08:00','13:00',TRUE),
-- Dr. Popescu (Medicina de Familie) — Luni-Vineri 11:00-17:00
(2,0,'11:00','17:00',TRUE),(2,1,'11:00','17:00',TRUE),(2,2,'11:00','17:00',TRUE),(2,3,'11:00','17:00',TRUE),(2,4,'11:00','17:00',TRUE),
-- Dr. Dumitru (Pediatrie) — Luni-Vineri 14:00-19:00
(3,0,'14:00','19:00',TRUE),(3,1,'14:00','19:00',TRUE),(3,2,'14:00','19:00',TRUE),(3,3,'14:00','19:00',TRUE),(3,4,'14:00','19:00',TRUE),
-- Dr. Constantin (Medicina Interna) — Luni-Vineri 08:00-15:00
(4,0,'08:00','15:00',TRUE),(4,1,'08:00','15:00',TRUE),(4,2,'08:00','15:00',TRUE),(4,3,'08:00','15:00',TRUE),(4,4,'08:00','15:00',TRUE),
-- Dr. Stanescu (Medicina de Familie) — Marti-Sambata 09:00-16:00
(5,1,'09:00','16:00',TRUE),(5,2,'09:00','16:00',TRUE),(5,3,'09:00','16:00',TRUE),(5,4,'09:00','16:00',TRUE),(5,5,'09:00','14:00',TRUE),
-- Dr. Munteanu (Pediatrie) — Luni-Vineri 10:00-18:00
(6,0,'10:00','18:00',TRUE),(6,1,'10:00','18:00',TRUE),(6,2,'10:00','18:00',TRUE),(6,3,'10:00','18:00',TRUE),(6,4,'10:00','18:00',TRUE),
-- Dr. Radulescu (Cardiologie) — Luni-Joi 08:00-16:00
(7,0,'08:00','16:00',TRUE),(7,1,'08:00','16:00',TRUE),(7,2,'08:00','16:00',TRUE),(7,3,'08:00','16:00',TRUE),
-- Dr. Florescu (Pneumologie) — Luni-Vineri 08:00-14:00
(8,0,'08:00','14:00',TRUE),(8,1,'08:00','14:00',TRUE),(8,2,'08:00','14:00',TRUE),(8,3,'08:00','14:00',TRUE),(8,4,'08:00','14:00',TRUE),
-- Dr. Popa (Gastroenterologie) — Marti-Sambata 12:00-19:00
(9,1,'12:00','19:00',TRUE),(9,2,'12:00','19:00',TRUE),(9,3,'12:00','19:00',TRUE),(9,4,'12:00','19:00',TRUE),(9,5,'10:00','15:00',TRUE),
-- Dr. Neagu (ORL) — Luni-Vineri 09:00-15:00
(10,0,'09:00','15:00',TRUE),(10,1,'09:00','15:00',TRUE),(10,2,'09:00','15:00',TRUE),(10,3,'09:00','15:00',TRUE),(10,4,'09:00','15:00',TRUE);


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
                                                                                                                (2,   50.00, '2025-03-01 08:00:00+02', 'CARD',     'FAILED',    'TXN-2025-FAIL');


INSERT INTO consultations (patient_id, status, complexity_level, emergency_redirect, notes) VALUES
                                                                                                (1,  'COMPLETED',         'SIMPLE',    FALSE, 'Upper respiratory infection — resolved with auto prescription'),
                                                                                                (2,  'COMPLETED',         'MEDIUM',    FALSE, 'Influenza — consultation with Dr. Popescu'),
                                                                                                (3,  'COMPLETED',         'COMPLEX',   FALSE, 'Cardiac patient with multiple comorbidities'),
                                                                                                (5,  'COMPLETED',         'MEDIUM',    FALSE, 'Food poisoning — gastroenterology referral'),
                                                                                                (6,  'SCHEDULED',         'MEDIUM',    FALSE, 'Scheduled for tomorrow'),
                                                                                                (7,  'DIAGNOSIS_PENDING', 'SIMPLE',    FALSE, 'Awaiting patient decision'),
                                                                                                (8,  'FORM_COMPLETED',    'MEDIUM',    FALSE, 'Form completed, awaiting diagnosis'),
                                                                                                (9,  'COMPLETED',         'SIMPLE',    FALSE, 'Cold — auto prescription issued'),
                                                                                                (10, 'CANCELLED',          NULL,       FALSE, 'Cancelled by patient'),
                                                                                                (1,  'COMPLETED',         'MEDIUM',    FALSE, 'Second consultation — influenza'),
                                                                                                (4,  'COMPLETED',         'SIMPLE',    FALSE, 'Cold — auto prescription'),
                                                                                                (2,  'SCHEDULED',         'COMPLEX',   FALSE, 'Complex cardiac case — scheduled with cardiologist'),
                                                                                                (11, 'COMPLETED',         'EMERGENCY', TRUE,  'Redirected to ER — possible appendicitis'),
                                                                                                (15, 'COMPLETED',         'MEDIUM',    FALSE, 'Child — acute otitis — ORL referral'),
                                                                                                (6,  'FORM_GENERATED',    'SIMPLE',    FALSE, 'Form generated, awaiting completion');


INSERT INTO consultation_symptoms (consultation_id, symptom_name, severity, order_index) VALUES
-- 1: cold SIMPLE
(1, 'fever',           'MILD',     1),
(1, 'headache',        'MILD',     2),
(1, 'cough',           'MODERATE', 3),
-- 2: flu MEDIUM
(2, 'high fever',      'SEVERE',   1),
(2, 'muscle pain',     'SEVERE',   2),
(2, 'extreme fatigue', 'SEVERE',   3),
-- 3: cardiac COMPLEX
(3, 'chest pain',               'SEVERE',   1),
(3, 'shortness of breath',      'SEVERE',   2),
(3, 'fatigue',                  'SEVERE',   3),
-- 4: food poisoning MEDIUM
(4, 'vomiting',        'SEVERE',   1),
(4, 'stomach pain',    'MODERATE', 2),
(4, 'fever',           'MODERATE', 3),
-- 5: scheduled digestive MEDIUM
(5, 'fever',           'MODERATE', 1),
(5, 'vomiting',        'MODERATE', 2),
(5, 'abdominal pain',  'MILD',     3),
-- 6
(6, 'headache',        'MODERATE', 1),
(6, 'fever',           'MILD',     2),
(6, 'cough',           'MILD',     3),
-- 7
(7, 'fever',           'MODERATE', 1),
(7, 'muscle pain',     'MODERATE', 2),
(7, 'headache',        'MODERATE', 3),
-- 8
(8, 'productive cough','MODERATE', 1),
(8, 'chest pain',      'MODERATE', 2),
(8, 'fever',           'MILD',     3),
-- 9: cold SIMPLE
(9, 'runny nose',      'MILD',     1),
(9, 'sore throat',     'MILD',     2),
(9, 'low fever',       'MILD',     3),
-- 10: cancelled
(10,'fatigue',         'MILD',     1),
(10,'headache',        'MILD',     2),
(10,'fever',           'MILD',     3),
-- 11: flu MEDIUM
(11,'high fever',      'SEVERE',   1),
(11,'muscle pain',     'SEVERE',   2),
(11,'chills',          'MODERATE', 3),
-- 12: cardiac COMPLEX
(12,'chest pain',               'SEVERE',   1),
(12,'shortness of breath',      'SEVERE',   2),
(12,'severe fatigue',           'SEVERE',   3),
-- 13: appendicitis EMERGENCY
(13,'severe abdominal pain',    'SEVERE',   1),
(13,'vomiting',                 'SEVERE',   2),
(13,'fever',                    'MODERATE', 3),
-- 14: child earache MEDIUM
(14,'fever',           'MODERATE', 1),
(14,'ear pain',        'SEVERE',   2),
(14,'no appetite',     'MODERATE', 3),
-- 15: simple
(15,'fever',           'MILD',     1),
(15,'cough',           'MILD',     2),
(15,'sore throat',     'MILD',     3);


INSERT INTO medical_form_questions (consultation_id, question_text, question_type, options, order_index, is_required) VALUES
-- consultatie 1 (cold)
(1, 'How many days have you had these symptoms?', 'MULTIPLE_CHOICE', '["1 day", "2–3 days", "4–7 days", "More than a week"]', 1, TRUE),
(1, 'How would you rate the overall intensity of your symptoms?', 'MULTIPLE_CHOICE', '["Mild — I can carry out daily activities normally", "Moderate — activities are affected", "Severe — I cannot carry out daily activities"]', 2, TRUE),
(1, 'What is your current temperature?', 'MULTIPLE_CHOICE', '["37–37.5 °C (low-grade)", "37.5–38.5 °C (moderate)", "38.5–39.5 °C (high)", "Above 39.5 °C"]', 3, TRUE),
(1, 'Do you have muscle or joint pain?', 'YES_NO', NULL, 4, TRUE),
(1, 'Do you have a sore throat or difficulty swallowing?', 'YES_NO', NULL, 5, TRUE),
-- consultatie 2 (flu)
(2, 'How many days have you had these symptoms?', 'MULTIPLE_CHOICE', '["1 day", "2–3 days", "4–7 days", "More than a week"]', 1, TRUE),
(2, 'What is your current temperature?', 'MULTIPLE_CHOICE', '["37–37.5 °C (low-grade)", "37.5–38.5 °C (moderate)", "38.5–39.5 °C (high)", "Above 39.5 °C"]', 2, TRUE),
(2, 'Do you have muscle or joint pain?', 'YES_NO', NULL, 3, TRUE),
(2, 'Are other people around you experiencing the same symptoms?', 'YES_NO', NULL, 4, TRUE),
(2, 'Have you been vaccinated against influenza this season?', 'YES_NO', NULL, 5, FALSE),
-- consultatie 3 (cardiac)
(3, 'How many days have you had these symptoms?', 'MULTIPLE_CHOICE', '["1 day", "2–3 days", "4–7 days", "More than a week"]', 1, TRUE),
(3, 'How would you rate the overall intensity of your symptoms?', 'MULTIPLE_CHOICE', '["Mild — I can carry out daily activities normally", "Moderate — activities are affected", "Severe — I cannot carry out daily activities"]', 2, TRUE),
(3, 'How would you describe the chest pain?', 'MULTIPLE_CHOICE', '["Dull / pressure-like", "Sharp / stabbing", "Burning", "Tightness"]', 3, TRUE),
(3, 'Does the pain radiate to your arm, jaw, or back?', 'YES_NO', NULL, 4, TRUE),
(3, 'Does the shortness of breath occur at rest or only during exertion?', 'MULTIPLE_CHOICE', '["At rest", "During light exertion", "Only during intense exertion"]', 5, TRUE),
(3, 'Have you experienced palpitations, dizziness, or fainting?', 'CHECKBOX', '["Palpitations", "Dizziness", "Fainting", "None of the above"]', 6, TRUE),
(3, 'What was your last recorded blood pressure reading?', 'OPEN_TEXT', NULL, 7, TRUE),
(3, 'Are you currently taking your prescribed cardiac / blood pressure medication?', 'YES_NO', NULL, 8, TRUE),
-- consultatie 4 (food poisoning)
(4, 'How many days have you had these symptoms?', 'MULTIPLE_CHOICE', '["1 day", "2–3 days", "4–7 days", "More than a week"]', 1, TRUE),
(4, 'Did you consume potentially spoiled food in the last 24 hours?', 'YES_NO', NULL, 2, TRUE),
(4, 'Do you have diarrhoea?', 'YES_NO', NULL, 3, TRUE),
(4, 'Are other people around you experiencing the same symptoms?', 'YES_NO', NULL, 4, TRUE),
(4, 'Is vomiting frequent (more than 3 episodes per day)?', 'YES_NO', NULL, 5, FALSE),
-- consultatie 13 (emergency)
(13,'Is the pain localized in the lower right side of the abdomen?', 'YES_NO', NULL, 1, TRUE),
(13,'How long have the symptoms been present?', 'MULTIPLE_CHOICE', '["Less than 1 hour", "1–6 hours", "6–12 hours", "More than 12 hours"]', 2, TRUE),
(13,'Has the pain progressively worsened since it started?', 'YES_NO', NULL, 3, TRUE),
(13,'Do you have a fever?', 'YES_NO', NULL, 4, TRUE),
-- consultatie 14 (child otitis)
(14,'Does the child have any skin rash or spots?', 'YES_NO', NULL, 1, TRUE),
(14,'Is the child pulling at their ears or complaining of ear pain?', 'YES_NO', NULL, 2, TRUE),
(14,'What is the child''s current temperature?', 'MULTIPLE_CHOICE', '["37–37.5 °C", "37.5–38.5 °C", "38.5–39.5 °C", "Above 39.5 °C"]', 3, TRUE),
(14,'Has the child been in contact with other ill children in the past 7 days?', 'YES_NO', NULL, 4, TRUE),
(14,'Is the child having difficulty breathing or breathing faster than normal?', 'YES_NO', NULL, 5, TRUE);


INSERT INTO medical_form_answers (question_id, consultation_id, answer_text) VALUES
-- consultatie 1 (cold)
(1,1,'2–3 days'),(2,1,'Mild — I can carry out daily activities normally'),(3,1,'37–37.5 °C (low-grade)'),(4,1,'No'),(5,1,'Yes'),
-- consultatie 2 (flu)
(6,2,'2–3 days'),(7,2,'Above 39.5 °C'),(8,2,'Yes'),(9,2,'Yes'),(10,2,'No'),
-- consultatie 3 (cardiac)
(11,3,'4–7 days'),(12,3,'Severe — I cannot carry out daily activities'),(13,3,'Dull / pressure-like'),(14,3,'Yes'),(15,3,'At rest'),(16,3,'Palpitations'),(17,3,'160/95 mmHg'),(18,3,'Yes'),
-- consultatie 4 (food poisoning)
(19,4,'1 day'),(20,4,'Yes'),(21,4,'Yes'),(22,4,'No'),(23,4,'Yes'),
-- consultatie 13 (emergency)
(24,13,'Yes'),(25,13,'1–6 hours'),(26,13,'Yes'),(27,13,'Yes'),
-- consultatie 14 (child otitis)
(28,14,'No'),(29,14,'Yes'),(30,14,'38.5–39.5 °C'),(31,14,'Yes'),(32,14,'No');


INSERT INTO diagnoses (consultation_id, diagnosis_name, diagnosis_type, complexity_level, icd_code, confidence_score, notes, created_by) VALUES
-- 1: cold
(1, 'Upper Respiratory Tract Infection (Cold)', 'AUTO_GENERATED', 'SIMPLE',    'J06', 78, NULL, NULL),
(1, 'Upper Respiratory Tract Infection (Cold)', 'CONFIRMED',      'SIMPLE',    'J06', 95, 'Confirmed, auto prescription issued', 3),
-- 2: flu
(2, 'Influenza (Flu)',                          'AUTO_GENERATED', 'MEDIUM',    'J10', 82, NULL, NULL),
(2, 'Influenza (Flu)',                          'CONFIRMED',      'MEDIUM',    'J10', 95, 'Confirmed by Dr. Popescu', 4),
-- 3: cardiac COMPLEX — preia Dr. Radulescu (Cardiologie, doctor_id=7)
(3, 'Possible Cardiac / Pulmonary Condition',   'AUTO_GENERATED', 'COMPLEX',   'I51', 72, 'High cardiac risk — specialist evaluation required', NULL),
(3, 'Acute Bronchitis',                         'AUTO_GENERATED', 'COMPLEX',   'J20', 38, NULL, NULL),
(3, 'Possible Cardiac / Pulmonary Condition',   'CONFIRMED',      'COMPLEX',   'I51', 90, 'Confirmed by Dr. Radulescu — patient referred to cardiology', 7),
-- 4: food poisoning — preia Dr. Popa (Gastroenterologie, doctor_id=9)
(4, 'Food Poisoning',                           'AUTO_GENERATED', 'MEDIUM',    'A05', 88, NULL, NULL),
(4, 'Food Poisoning',                           'CONFIRMED',      'MEDIUM',    'A05', 95, 'Confirmed by Dr. Popa', 9),
-- 9: cold
(9, 'Upper Respiratory Tract Infection (Cold)', 'AUTO_GENERATED', 'SIMPLE',    'J06', 75, NULL, NULL),
(9, 'Upper Respiratory Tract Infection (Cold)', 'CONFIRMED',      'SIMPLE',    'J06', 90, 'Auto prescription confirmed', 5),
-- 11: flu
(11,'Influenza (Flu)',                          'AUTO_GENERATED', 'MEDIUM',    'J10', 85, NULL, NULL),
(11,'Influenza (Flu)',                          'CONFIRMED',      'MEDIUM',    'J10', 92, 'Confirmed by Dr. Ionescu', 3),
-- 13: emergency
(13,'Possible Acute Appendicitis — requires emergency surgical evaluation', 'AUTO_GENERATED', 'EMERGENCY', 'K37', 75, 'Patient redirected to the nearest ER.', NULL),
-- 14: child otitis — preia Dr. Neagu (ORL, doctor_id=10)
(14,'Acute Otitis Media',                       'AUTO_GENERATED', 'MEDIUM',    'H66', 80, NULL, NULL),
(14,'Acute Otitis Media',                       'CONFIRMED',      'MEDIUM',    'H66', 95, 'Confirmed by Dr. Neagu — ORL specialist', 10);


INSERT INTO appointments (consultation_id, doctor_id, patient_id, start_time, end_time, duration_minutes, status, notes) VALUES
                                                                                                                             (2,  2,  2,  '2025-04-10 11:00:00+02', '2025-04-10 11:20:00+02', 20, 'COMPLETED', 'Influenza — consultation completed with Dr. Popescu (Medicina de Familie)'),
                                                                                                                             (3,  7,  3,  '2025-04-08 09:00:00+02', '2025-04-08 09:30:00+02', 30, 'COMPLETED', 'Cardiac case — evaluated by Dr. Radulescu (Cardiologie), patient referred to hospital'),
                                                                                                                             (4,  9,  4,  '2025-04-05 13:00:00+02', '2025-04-05 13:20:00+02', 20, 'COMPLETED', 'Food poisoning — Dr. Popa (Gastroenterologie), outpatient treatment'),
                                                                                                                             (5,  9,  5,  '2025-05-06 14:00:00+02', '2025-05-06 14:20:00+02', 20, 'SCHEDULED', 'Digestive symptoms — scheduled with gastroenterologist'),
                                                                                                                             (11, 1,  1,  '2025-04-15 08:30:00+02', '2025-04-15 08:50:00+02', 20, 'COMPLETED', 'Influenza — Dr. Ionescu (Medicina Interna)'),
                                                                                                                             (12, 7,  2,  '2025-05-08 09:00:00+02', '2025-05-08 09:30:00+02', 30, 'SCHEDULED', 'Complex cardiac case — Dr. Radulescu (Cardiologie)'),
                                                                                                                             (14, 10, 14, '2025-04-20 09:00:00+02', '2025-04-20 09:20:00+02', 20, 'COMPLETED', 'Child otitis — Dr. Neagu (ORL specialist)');


INSERT INTO prescriptions (consultation_id, patient_id, diagnosis_id, issued_at, valid_until, is_auto_generated) VALUES
                                                                                                                     (1,  1,  2,  '2025-04-02 10:30:00+02', '2025-04-09 10:30:00+02', TRUE),
                                                                                                                     (2,  2,  4,  '2025-04-10 11:25:00+02', '2025-04-17 11:25:00+02', FALSE),
                                                                                                                     (4,  4,  9,  '2025-04-05 13:25:00+02', '2025-04-12 13:25:00+02', FALSE),
                                                                                                                     (9,  9,  11, '2025-04-18 15:00:00+02', '2025-04-25 15:00:00+02', TRUE),
                                                                                                                     (11, 1,  13, '2025-04-15 08:55:00+02', '2025-04-22 08:55:00+02', FALSE),
                                                                                                                     (14, 14, 16, '2025-04-20 09:25:00+02', '2025-04-30 09:25:00+02', FALSE);


INSERT INTO prescription_medications (prescription_id, medication_name, dosage, frequency, duration_days, instructions) VALUES
-- 1: cold auto
(1,'Paracetamol 500 mg',              '1 tablet',  'Every 6–8 hours as needed', 5, 'Do not exceed 4 tablets per day. Take with food.'),
(1,'Vitamin C 1000 mg (effervescent)','1 tablet',  'Once daily',                7, 'Dissolve in water. Take after meals.'),
(1,'Ibuprofen 400 mg',                '1 tablet',  'Every 8 hours as needed',   3, 'Take with food. Contraindicated in peptic ulcer.'),
-- 2: flu doctor
(2,'Paracetamol 1000 mg',             '1 tablet',  'Every 8 hours',             5, 'Do not exceed 3 tablets per day.'),
(2,'Vitamin C 1000 mg',               '1 tablet',  'Once daily',               10, 'After meals.'),
(2,'Nasal saline spray',              '2 puffs per nostril', 'Four times daily', 7, 'Before each meal.'),
-- 3: food poisoning
(3,'Diosmectite (Smecta)',            '1 sachet',  'Three times daily',          3, 'Dissolve in 100ml water. Between meals.'),
(3,'Oral Rehydration Solution',       '200–400 ml','After each episode',         3, 'Sip slowly. Essential for rehydration.'),
(3,'Paracetamol 500 mg',              '1 tablet',  'Every 6–8 hours as needed',  3, 'Only if fever is present.'),
-- 4: cold auto
(4,'Paracetamol 500 mg',              '1 tablet',  'Every 6–8 hours as needed',  5, 'Do not exceed 4 tablets per day.'),
(4,'Vitamin C 1000 mg (effervescent)','1 tablet',  'Once daily',                 7, 'After meals.'),
-- 5: flu
(5,'Paracetamol 1000 mg',             '1 tablet',  'Every 8 hours',              5, 'Do not exceed 3 tablets per day.'),
(5,'Vitamin C 1000 mg',               '1 tablet',  'Once daily',                10, 'After meals.'),
(5,'Ibuprofen 400 mg',                '1 tablet',  'Every 8 hours as needed',    3, 'Take with food.'),
-- 6: child otitis
(6,'Paracetamol syrup 240mg/5ml',     '5 ml',      'Every 6–8 hours as needed',  5, 'Weight-based dosing. Do not exceed recommended daily dose.'),
(6,'Amoxicillin suspension 250mg/5ml','5 ml',      'Three times daily',          7, 'Complete the full course. Take with or without food.'),
(6,'Otipax ear drops',                '4 drops per ear', 'Twice daily',          5, 'Warm the bottle in hands before use. Do not use if eardrum is perforated.');


ALTER TABLE consultations         ENABLE TRIGGER ALL;
ALTER TABLE consultation_symptoms ENABLE TRIGGER ALL;
ALTER TABLE medical_form_answers  ENABLE TRIGGER ALL;
ALTER TABLE diagnoses             ENABLE TRIGGER ALL;
ALTER TABLE appointments          ENABLE TRIGGER ALL;
ALTER TABLE subscriptions         ENABLE TRIGGER ALL;
ALTER TABLE payment_history       ENABLE TRIGGER ALL;
ALTER TABLE patients              ENABLE TRIGGER ALL;
ALTER TABLE guardians             ENABLE TRIGGER ALL;