package com.telemedicina.patient.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * DTO primit de la client la crearea/actualizarea profilului.
 * Validările @Valid sunt primul nivel de apărare — DB-ul are propriile
 * CHECK constraints, dar e mai eficient să respingem datele proaste înainte
 * să ajungă la bază.
 */
@Getter
@Setter
@NoArgsConstructor
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

    //optionale
    @Pattern(regexp = "A\\+|A-|B\\+|B-|AB\\+|AB-|O\\+|O-",
            message = "Grupă sanguină invalidă")
    private String bloodType;

    @Pattern(regexp = "^\\+?[0-9\\s\\-]{7,20}$",
            message = "Număr de telefon invalid")
    private String phone;

    @Pattern(regexp = "^[0-9]{13}$", message = "CNP-ul trebuie să aibă exact 13 cifre")
    private String cnp;

    private String address;
}