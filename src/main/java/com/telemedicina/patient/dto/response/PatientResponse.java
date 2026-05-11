package com.telemedicina.patient.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * DTO returnat clientului.
 * Include `age` și `ageCategory` calculate de DB —
 * clientul nu trebuie să le calculeze el
 */
@Getter
@Setter
@NoArgsConstructor
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
}