package com.telemedicina.patient.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * DTO returnat clientului.
 * Include `age` și `ageCategory` calculate de DB —
 * clientul nu trebuie să le calculeze el însuși.
 */
public class PatientResponse {
    private Long id;
    private Long userId;
    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    private Integer age;
    private String ageCategory;
    private String gender;
    private String bloodType;
    private String phone;
    private String cnp;
    private String address;
    private boolean isGuardian;
    private OffsetDateTime createdAt;

    public PatientResponse() {}

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
}