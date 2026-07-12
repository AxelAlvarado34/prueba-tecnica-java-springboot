package com.axel.alvarado.coworking_service.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

public record ReservationRequest(
        @NotNull(message = "El id del espacio es obligatorio") Long spaceId,

        @NotNull @Future(message = "La fecha de inicio debe ser futura") LocalDateTime startDateTime,

        @NotNull @Future(message = "La fecha de fin debe ser futura") LocalDateTime endDateTime) {

}
