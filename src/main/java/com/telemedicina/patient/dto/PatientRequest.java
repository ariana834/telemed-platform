package com.telemedicina.patient.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * DTO primit de la client la crearea/actualizarea profilului.
 * Validările @Valid sunt primul nivel de apărare — DB-ul are propriile
 * CHECK constraints, dar e mai eficient să respingem datele proaste înainte
 * să ajungă la bază.
 */
public class PatientRequest {

    @NotBlank(message = "Prenumele este obligatoriu")
    @Size(max = 100)
    private String firstName;

    @NotBlank(message = "Numele este obligatoriu")
    @Size(max = 100)
    private String lastName;

    @NotNull(message = "Data nașterii este obligatorie")
    @Past(message = "Data nașterii trebuie să fie în trecut")
    private LocalDate birthDate;

    @NotBlank(message = "Genul este obligatoriu")
    @Pattern(regexp = "MALE|FEMALE|OTHER", message = "Gen invalid. Valori acceptate: MALE, FEMALE, OTHER")
    private String gender;

    // Opționale — nu toți pacienții le completează la înregistrare
    @Pattern(regexp = "A\\+|A-|B\\+|B-|AB\\+|AB-|O\\+|O-",
            message = "Grupă sanguină invalidă")
    private String bloodType;

    @Pattern(regexp = "^\\+?[0-9\\s\\-]{7,20}$",
            message = "Număr de telefon invalid")
    private String phone;

    @Pattern(regexp = "^[0-9]{13}$", message = "CNP-ul trebuie să aibă exact 13 cifre")
    private String cnp;

    private String address;

    public PatientRequest() {}

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }

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
}