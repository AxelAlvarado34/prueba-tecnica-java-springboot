package com.axel.alvarado.coworking_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank @Email(message = "Debe ser un email valido") String email,

        @NotBlank String password) {
}
