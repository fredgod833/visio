package com.example.chatvideo.payload.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {

    @Size(max = 50)
    @Email
    private String email;

    @Size(max = 30)
    private String firstName;

    @Size(max = 30)
    private String lastName;

    @NotBlank
    @Size(min = 1, max = 40)
    private String password;

    @NotBlank
    @Size(min = 1, max = 40)
    private String newPassword;


}
