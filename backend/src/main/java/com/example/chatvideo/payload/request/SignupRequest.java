package com.example.chatvideo.payload.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SignupRequest {

    @Size(max = 50, message = "Votre email ne doit pas dépasser 50 caratères.")
    @Email
    private String email;

    @Size(min = 3, max = 20, message = "Votre prénom doit avoir entre 3 et 20 caratères.")
    private String firstName;

    @Size(min = 3, max = 20, message = "Votre nom doit avoir entre 3 et 20 caratères.")
    private String lastName;

    @Size(min = 8, max = 20, message = "Votre mot de passe doit avoir entre 8 et 20 caratères.")
    private String password;

}
