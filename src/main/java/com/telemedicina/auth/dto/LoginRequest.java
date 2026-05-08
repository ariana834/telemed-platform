package com.telemedicina.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class LoginRequest {

    @NotBlank(message = "Email-ul este obligatoriu")
    @Email(message = "Format email invalid")
    private String email;

    @NotBlank(message = "Parola este obligatorie")
    private String password;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}