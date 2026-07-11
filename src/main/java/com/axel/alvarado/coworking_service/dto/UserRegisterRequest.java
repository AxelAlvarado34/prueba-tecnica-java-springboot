package com.axel.alvarado.coworking_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRegisterRequest(
        @NotBlank @Email(message = "Debe ser un email valido") String email,

        @NotBlank @Size(min = 8, message = "La contrasena debe tener minimo 8 caracteres") String password) {

}
