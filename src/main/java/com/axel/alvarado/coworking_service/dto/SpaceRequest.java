package com.axel.alvarado.coworking_service.dto;

import java.math.BigDecimal;

import com.axel.alvarado.coworking_service.enums.SpaceType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SpaceRequest(
        @NotBlank(message = "El nombre es obligatorio") String name,

        @NotNull(message = "El tipo de espacio es obligatorio") SpaceType type,

        @NotNull @Positive(message = "La capacidad debe ser mayor a 0") Integer capacity,

        @NotBlank(message = "La ubicacion es obligatoria") String location,

        @NotNull @Positive(message = "La tarifa debe ser mayor a 0") BigDecimal hourlyRate) {
}
