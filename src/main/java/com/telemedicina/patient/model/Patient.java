package com.telemedicina.patient.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Modelul pacientului — oglindă 1:1 a tabelei `patients` din DB.
 * Câmpul `age` vine calculat direct din DB (GENERATED ALWAYS AS STORED),
 * la fel și `ageCategory` setat automat de triggerul trg_calculate_age_category.
 * Nu le calculăm niciodată în Java — DB-ul e sursa de adevăr.
 */
@Getter
@Setter
@NoArgsConstructor
public class Patient {
    private Long id;
    private Long userId;
    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    private Integer age;            // calculat de DB, readonly
    private String ageCategory;     // CHILD / ADULT / SENIOR — setat de trigger
    private String gender;          // MALE / FEMALE / OTHER
    private String bloodType;
    private String phone;
    private String cnp;
    private String address;
    private boolean isGuardian;     // TRUE dacă userul are un copil înregistrat
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}