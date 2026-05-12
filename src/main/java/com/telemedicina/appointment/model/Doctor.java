package com.telemedicina.appointment.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Doctor {
    private Long id;
    private Long userId;
    private String firstName;
    private String lastName;
    private String specialization;
    private String licenseNumber;
    private String phone;
    private String bio;
    private Boolean isAvailable;
    private Instant createdAt;
}