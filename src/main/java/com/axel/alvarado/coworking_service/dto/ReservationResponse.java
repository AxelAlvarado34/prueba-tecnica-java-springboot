package com.axel.alvarado.coworking_service.dto;

import java.time.LocalDateTime;

import com.axel.alvarado.coworking_service.enums.ReservationState;

public record ReservationResponse(
        Long id,
        Long spaceId,
        String spaceName,
        Long userId,
        String userEmail,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        ReservationState status) {

}
