package com.telemedicina.patient.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Modelul pacientului — oglindă 1:1 a tabelei `patients` din DB.
 * Câmpul `age` vine calculat direct din DB (GENERATED ALWAYS AS STORED),
 * la fel și `ageCategory` setat automat de triggerul trg_calculate_age_category.
 * Nu le calculăm niciodată în Java — DB-ul e sursa de adevăr.
 */
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

    public Patient() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public String getAgeCategory() { return ageCategory; }
    public void setAgeCategory(String ageCategory) { this.ageCategory = ageCategory; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getBloodType() { return bloodType; }
    public void setBloodType(String bloodType) { this.bloodType = bloodType; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getCnp() { return cnp; }
    public void setCnp(String cnp) { this.cnp = cnp; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public boolean isGuardian() { return isGuardian; }
    public void setGuardian(boolean guardian) { isGuardian = guardian; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}